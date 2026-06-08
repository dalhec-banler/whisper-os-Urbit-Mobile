package io.nativeplanet.controller;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructTimeval;
import android.system.UnixSocketAddress;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Client for vere's conn.sock control plane.
 * Uses android.system.Os for direct AF_UNIX socket operations.
 */
public class ConnSockClient implements AutoCloseable {

    private static final String TAG = "ConnSockClient";
    private static final int SOCKET_TIMEOUT_MS = 5000;

    private final String sockPath;
    private FileDescriptor fd;
    private final AtomicLong requestIdCounter = new AtomicLong(1);

    private static final String GRACEFUL_EXIT_HOON =
            "=/  m  (strand ,vase)  " +
            ";<  our=@p  bind:m  get-our  " +
            ";<  ~  bind:m  (poke [our %hood] %drum-exit !>(~))  " +
            "(pure:m !>('success'))";

    public ConnSockClient(String sockPath) {
        this.sockPath = sockPath;
    }

    public void connect() throws IOException {
        if (fd != null && fd.valid()) {
            return;
        }

        try {
            fd = Os.socket(OsConstants.AF_UNIX, OsConstants.SOCK_STREAM, 0);
            Log.d(TAG, "Created socket fd=" + fd);

            // Set socket timeouts to prevent blocking forever
            StructTimeval timeout = StructTimeval.fromMillis(SOCKET_TIMEOUT_MS);
            Os.setsockoptTimeval(fd, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, timeout);
            Os.setsockoptTimeval(fd, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, timeout);

            UnixSocketAddress address = UnixSocketAddress.createFileSystem(sockPath);
            Log.d(TAG, "Connecting to " + sockPath);

            Os.connect(fd, address);
            Log.d(TAG, "Connected to " + sockPath);

        } catch (ErrnoException e) {
            Log.e(TAG, "Socket operation failed: errno=" + e.errno + " (" + errnoName(e.errno) + "): " + e.getMessage(), e);
            closeQuietly();
            throw new IOException("Socket connect failed: " + errnoName(e.errno), e);
        }
    }

    public boolean isConnected() {
        return fd != null && fd.valid();
    }

    @Override
    public void close() {
        closeQuietly();
    }

    private void closeQuietly() {
        if (fd != null) {
            try {
                Os.close(fd);
            } catch (ErrnoException e) {
                Log.w(TAG, "close() failed: " + e.getMessage());
            }
            fd = null;
        }
    }

    /**
     * Send a %peel request and receive response.
     */
    public NounCodec.Noun sendPeel(String command) throws IOException {
        if (fd == null || !fd.valid()) {
            throw new IOException("Not connected");
        }

        long rid = requestIdCounter.getAndIncrement();
        NounCodec.Noun request = NounCodec.buildPeelRequest(rid, command);
        return sendRequest(request);
    }

    /**
     * Send a %fyrd %khan-eval request and receive response.
     */
    public NounCodec.Noun sendKhanEval(String hoon) throws IOException {
        if (fd == null || !fd.valid()) {
            throw new IOException("Not connected");
        }

        long rid = requestIdCounter.getAndIncrement();
        NounCodec.Noun request = NounCodec.buildFyrdKhanEvalRequest(rid, hoon);
        return sendRequest(request);
    }

    /**
     * Request graceful Urbit shutdown using the same %hood %drum-exit
     * path used by GroundSeg's Click |exit implementation.
     */
    public boolean requestGracefulExit() throws IOException {
        NounCodec.Noun response = sendKhanEval(GRACEFUL_EXIT_HOON);
        return NounCodec.parseFyrdSuccessResponse(response);
    }

    private NounCodec.Noun sendRequest(NounCodec.Noun request) throws IOException {
        // Jam and frame
        BigInteger jammed = NounCodec.jam(request);
        byte[] framed = NounCodec.newtEncode(jammed);

        // Send
        writeAll(framed);

        // Read response
        byte[] response = readNewtFrame();
        if (response == null) {
            throw new IOException("No response received");
        }

        // Decode
        BigInteger responseJammed = NounCodec.newtDecode(response);
        if (responseJammed == null) {
            throw new IOException("Invalid newt frame");
        }

        return NounCodec.cue(responseJammed);
    }

    private void writeAll(byte[] data) throws IOException {
        int offset = 0;
        while (offset < data.length) {
            try {
                int written = Os.write(fd, data, offset, data.length - offset);
                if (written <= 0) {
                    throw new IOException("Write failed: returned " + written);
                }
                offset += written;
            } catch (ErrnoException e) {
                if (e.errno == OsConstants.EAGAIN) {
                    throw new SocketTimeoutException("Write timeout");
                }
                throw new IOException("Write failed: " + errnoName(e.errno), e);
            }
        }
    }

    private byte[] readNewtFrame() throws IOException {
        // Read 5-byte header
        byte[] header = new byte[5];
        readFully(header, 0, 5);

        // Parse length
        if (header[0] != 0x00) {
            throw new IOException("Invalid newt version byte: " + header[0]);
        }

        int len = (header[1] & 0xFF) |
                  ((header[2] & 0xFF) << 8) |
                  ((header[3] & 0xFF) << 16) |
                  ((header[4] & 0xFF) << 24);

        if (len > 1024 * 1024) {
            throw new IOException("Response too large: " + len);
        }

        // Read payload
        byte[] payload = new byte[len];
        readFully(payload, 0, len);

        // Return full frame for newtDecode
        byte[] frame = new byte[5 + len];
        System.arraycopy(header, 0, frame, 0, 5);
        System.arraycopy(payload, 0, frame, 5, len);
        return frame;
    }

    private void readFully(byte[] buffer, int offset, int length) throws IOException {
        int totalRead = 0;
        while (totalRead < length) {
            try {
                int n = Os.read(fd, buffer, offset + totalRead, length - totalRead);
                if (n < 0) {
                    throw new IOException("Connection closed while reading");
                }
                if (n == 0) {
                    throw new IOException("Read returned 0");
                }
                totalRead += n;
            } catch (ErrnoException e) {
                if (e.errno == OsConstants.EAGAIN) {
                    throw new SocketTimeoutException("Read timeout");
                }
                throw new IOException("Read failed: " + errnoName(e.errno), e);
            }
        }
    }

    private static String errnoName(int errno) {
        if (errno == OsConstants.ECONNREFUSED) return "ECONNREFUSED";
        if (errno == OsConstants.EACCES) return "EACCES";
        if (errno == OsConstants.EPERM) return "EPERM";
        if (errno == OsConstants.ENOENT) return "ENOENT";
        if (errno == OsConstants.ETIMEDOUT) return "ETIMEDOUT";
        if (errno == OsConstants.EAGAIN) return "EAGAIN";
        if (errno == OsConstants.EINTR) return "EINTR";
        if (errno == OsConstants.ENOTCONN) return "ENOTCONN";
        if (errno == OsConstants.EBADF) return "EBADF";
        return "errno=" + errno;
    }

    // --- Convenience methods ---

    /**
     * Check if ship is alive via %peel %live.
     */
    public boolean checkLive() throws IOException {
        NounCodec.Noun response = sendPeel("live");
        return NounCodec.parseLiveResponse(response);
    }

    /**
     * Get ship @p via %peel %who.
     */
    public BigInteger getShipId() throws IOException {
        NounCodec.Noun response = sendPeel("who");
        return NounCodec.parseWhoResponse(response);
    }

    /**
     * Get vere version via %peel %v.
     */
    public String getVersion() throws IOException {
        NounCodec.Noun response = sendPeel("v");
        return NounCodec.parseVersionResponse(response);
    }

    /**
     * Result of a full status poll.
     */
    public static class StatusResult {
        public final boolean connected;
        public final boolean alive;
        public final BigInteger shipId;
        public final String version;
        public final String error;

        private StatusResult(boolean connected, boolean alive, BigInteger shipId,
                            String version, String error) {
            this.connected = connected;
            this.alive = alive;
            this.shipId = shipId;
            this.version = version;
            this.error = error;
        }

        public static StatusResult success(boolean alive, BigInteger shipId, String version) {
            return new StatusResult(true, alive, shipId, version, null);
        }

        public static StatusResult connectionFailed(String error) {
            return new StatusResult(false, false, null, null, error);
        }

        public static StatusResult protocolError(String error) {
            return new StatusResult(true, false, null, null, error);
        }
    }

    /**
     * Perform a full status poll: connect, query live/who/v, close.
     */
    public static StatusResult pollStatus(String sockPath) {
        // Check socket file exists first
        File sockFile = new File(sockPath);
        if (!sockFile.exists()) {
            return StatusResult.connectionFailed("SOCK_MISSING");
        }

        try (ConnSockClient client = new ConnSockClient(sockPath)) {
            client.connect();

            boolean alive = client.checkLive();
            BigInteger shipId = null;
            String version = null;

            if (alive) {
                try {
                    shipId = client.getShipId();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to get ship id: " + e.getMessage());
                }

                try {
                    version = client.getVersion();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to get version: " + e.getMessage());
                }
            }

            return StatusResult.success(alive, shipId, version);

        } catch (SocketTimeoutException e) {
            return StatusResult.connectionFailed("TIMEOUT");
        } catch (IOException e) {
            String msg = e.getMessage();
            Throwable cause = e.getCause();
            Log.e(TAG, "Poll failed: " + e.getClass().getName() + ": " + msg, e);

            // Map errno to normalized error codes
            if (cause instanceof ErrnoException) {
                int errno = ((ErrnoException) cause).errno;
                if (errno == OsConstants.ECONNREFUSED) {
                    return StatusResult.connectionFailed("CONN_REFUSED");
                } else if (errno == OsConstants.EACCES || errno == OsConstants.EPERM) {
                    return StatusResult.connectionFailed("PERMISSION_DENIED");
                } else if (errno == OsConstants.ENOENT) {
                    return StatusResult.connectionFailed("SOCK_MISSING");
                } else if (errno == OsConstants.ETIMEDOUT) {
                    return StatusResult.connectionFailed("TIMEOUT");
                }
            }

            if (msg != null && msg.contains("Invalid newt")) {
                return StatusResult.protocolError("MALFORMED_FRAME");
            }
            return StatusResult.connectionFailed("IO_ERROR: " + msg);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Protocol error: " + e.getMessage(), e);
            return StatusResult.protocolError("CUE_FAILED");
        } catch (Exception e) {
            Log.e(TAG, "Unknown error: " + e.getMessage(), e);
            return StatusResult.protocolError("UNKNOWN");
        }
    }
}

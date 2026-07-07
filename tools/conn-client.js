#!/usr/bin/env node
/*
 * Minimal conn.sock client for NativePlanet development.
 *
 * Uses newt framing plus jam/cue over a TCP socket. For Android device tests,
 * pass --adb and the tool will forward the current boot-package pier's
 * .urb/conn.sock to localhost.
 */

const childProcess = require("child_process");
const net = require("net");

const DEFAULT_PORT = 12321;

class Atom {
  constructor(value) {
    this.value = BigInt(value);
  }

  static cord(text) {
    if (!text) return new Atom(0n);
    const bytes = Buffer.from(text, "utf8");
    let value = 0n;
    for (let i = 0; i < bytes.length; i++) {
      value |= BigInt(bytes[i]) << BigInt(i * 8);
    }
    return new Atom(value);
  }

  toCord() {
    if (this.value === 0n) return "";
    const bytes = [];
    let value = this.value;
    while (value > 0n) {
      bytes.push(Number(value & 0xffn));
      value >>= 8n;
    }
    return Buffer.from(bytes).toString("utf8");
  }
}

class Cell {
  constructor(head, tail) {
    this.head = head;
    this.tail = tail;
  }
}

const nil = new Atom(0n);

function list(items) {
  if (items.length === 0) return nil;
  let noun = items[items.length - 1];
  for (let i = items.length - 2; i >= 0; i--) {
    noun = new Cell(items[i], noun);
  }
  return noun;
}

function isAtom(noun) {
  return noun instanceof Atom;
}

function isCell(noun) {
  return noun instanceof Cell;
}

function matInto(atom, bits) {
  if (atom === 0n) {
    bits.push(true);
    return;
  }

  const bitLen = atom.toString(2).length;
  const lenLen = bitLen.toString(2).length;

  for (let i = 0; i < lenLen; i++) bits.push(false);
  bits.push(true);

  for (let i = 0; i < lenLen - 1; i++) {
    bits.push((bitLen & (1 << i)) !== 0);
  }

  for (let i = 0; i < bitLen; i++) {
    bits.push(((atom >> BigInt(i)) & 1n) === 1n);
  }
}

function jam(noun) {
  const bits = [];
  const cache = new Map();

  function key(n) {
    if (isAtom(n)) return `a:${n.value.toString()}`;
    return `c:${key(n.head)}:${key(n.tail)}`;
  }

  function inner(n, pos) {
    const k = key(n);
    if (cache.has(k) && cache.get(k) < pos) {
      bits.push(true, true);
      matInto(BigInt(cache.get(k)), bits);
      return;
    }
    cache.set(k, pos);

    if (isAtom(n)) {
      bits.push(false);
      matInto(n.value, bits);
      return;
    }

    bits.push(true, false);
    inner(n.head, bits.length);
    inner(n.tail, bits.length);
  }

  inner(noun, 0);

  let value = 0n;
  for (let i = 0; i < bits.length; i++) {
    if (bits[i]) value |= 1n << BigInt(i);
  }
  return value;
}

function rub(jammed, pos) {
  let zeros = 0;
  while (((jammed >> BigInt(pos + zeros)) & 1n) === 0n) {
    zeros++;
    if (zeros > 64) throw new Error("oversized atom length");
  }

  if (zeros === 0) {
    return { consumed: 1, value: 0n };
  }

  const lenLen = zeros;
  let bitLen = 1 << (lenLen - 1);
  for (let i = 0; i < lenLen - 1; i++) {
    if (((jammed >> BigInt(pos + lenLen + 1 + i)) & 1n) === 1n) {
      bitLen |= 1 << i;
    }
  }

  let value = 0n;
  const dataStart = pos + lenLen + lenLen;
  for (let i = 0; i < bitLen; i++) {
    if (((jammed >> BigInt(dataStart + i)) & 1n) === 1n) {
      value |= 1n << BigInt(i);
    }
  }

  return { consumed: lenLen + lenLen + bitLen, value };
}

function cue(jammed) {
  const cache = new Map();

  function inner(pos) {
    if (((jammed >> BigInt(pos)) & 1n) === 0n) {
      const r = rub(jammed, pos + 1);
      const noun = new Atom(r.value);
      cache.set(pos, noun);
      return { consumed: 1 + r.consumed, noun };
    }

    if (((jammed >> BigInt(pos + 1)) & 1n) === 0n) {
      const head = inner(pos + 2);
      const tail = inner(pos + 2 + head.consumed);
      const noun = new Cell(head.noun, tail.noun);
      cache.set(pos, noun);
      return { consumed: 2 + head.consumed + tail.consumed, noun };
    }

    const r = rub(jammed, pos + 2);
    const noun = cache.get(Number(r.value));
    if (!noun) throw new Error(`invalid back-reference at ${pos}`);
    return { consumed: 2 + r.consumed, noun };
  }

  return inner(0).noun;
}

function bigintToBytes(value) {
  if (value === 0n) return Buffer.from([0]);
  const bytes = [];
  while (value > 0n) {
    bytes.push(Number(value & 0xffn));
    value >>= 8n;
  }
  return Buffer.from(bytes);
}

function bytesToBigint(buffer) {
  let value = 0n;
  for (let i = 0; i < buffer.length; i++) {
    value |= BigInt(buffer[i]) << BigInt(i * 8);
  }
  return value;
}

function newtEncode(noun) {
  const payload = bigintToBytes(jam(noun));
  const frame = Buffer.alloc(5 + payload.length);
  frame[0] = 0;
  frame.writeUInt32LE(payload.length, 1);
  payload.copy(frame, 5);
  return frame;
}

function newtDecode(frame) {
  if (frame.length < 5 || frame[0] !== 0) {
    throw new Error("invalid newt frame");
  }
  const length = frame.readUInt32LE(1);
  if (frame.length < 5 + length) {
    throw new Error("incomplete newt frame");
  }
  return cue(bytesToBigint(frame.subarray(5, 5 + length)));
}

function buildPeel(requestId, command) {
  return new Cell(
    new Atom(requestId),
    new Cell(Atom.cord("peel"), new Cell(Atom.cord(command), nil)),
  );
}

function buildKhanEval(requestId, hoon) {
  return list([
    new Atom(requestId),
    Atom.cord("fyrd"),
    Atom.cord("base"),
    Atom.cord("khan-eval"),
    Atom.cord("noun"),
    Atom.cord("ted-eval"),
    Atom.cord(hoon),
  ]);
}

function atomEqualsCord(noun, cord) {
  return isAtom(noun) && noun.toCord() === cord;
}

function atomEquals(noun, value) {
  return isAtom(noun) && noun.value === BigInt(value);
}

function pretty(noun) {
  if (isAtom(noun)) {
    const cord = noun.toCord();
    if (/^[a-z0-9-]{1,32}$/.test(cord)) return `%${cord}`;
    if (/^[\x20-\x7e]{1,120}$/.test(cord)) return JSON.stringify(cord);
    return noun.value.toString();
  }
  const parts = [];
  let cursor = noun;
  while (isCell(cursor)) {
    parts.push(pretty(cursor.head));
    cursor = cursor.tail;
  }
  if (!atomEquals(cursor, 0n)) {
    parts.push(".", pretty(cursor));
  }
  return `[${parts.join(" ")}]`;
}

function parsePeel(noun) {
  if (!isCell(noun)) throw new Error("peel response not a cell");
  const rid = noun.head.value.toString();
  const result = noun.tail;
  if (atomEquals(result, 0n)) return { rid, hasValue: false, value: null };
  if (isCell(result) && atomEquals(result.head, 0n)) {
    return { rid, hasValue: true, value: result.tail };
  }
  return { rid, hasValue: true, value: result };
}

function parseFyrdNounValue(noun) {
  if (!isCell(noun) || !isCell(noun.tail)) {
    throw new Error("fyrd response not tagged");
  }
  const tagged = noun.tail;
  if (!atomEqualsCord(tagged.head, "avow") || !isCell(tagged.tail)) {
    throw new Error(`fyrd response is not %avow: ${pretty(noun)}`);
  }
  const result = tagged.tail;
  if (!atomEquals(result.head, 0n) || !isCell(result.tail)) {
    throw new Error(`fyrd thread failed: ${pretty(noun)}`);
  }
  const page = result.tail;
  if (!atomEqualsCord(page.head, "noun")) {
    throw new Error(`fyrd response mark is not %noun: ${pretty(noun)}`);
  }
  return page.tail;
}

function readFrame(socket) {
  return new Promise((resolve, reject) => {
    let buffer = Buffer.alloc(0);
    let expected = null;
    const timer = setTimeout(() => {
      cleanup();
      reject(new Error("timed out waiting for conn.sock response"));
    }, 10000);

    function cleanup() {
      clearTimeout(timer);
      socket.off("data", onData);
      socket.off("error", onError);
      socket.off("end", onEnd);
    }

    function onError(error) {
      cleanup();
      reject(error);
    }

    function onEnd() {
      cleanup();
      reject(new Error("conn.sock closed before response"));
    }

    function onData(chunk) {
      buffer = Buffer.concat([buffer, chunk]);
      if (expected === null && buffer.length >= 5) {
        if (buffer[0] !== 0) {
          cleanup();
          reject(new Error(`invalid newt version ${buffer[0]}`));
          return;
        }
        expected = 5 + buffer.readUInt32LE(1);
      }
      if (expected !== null && buffer.length >= expected) {
        cleanup();
        resolve(buffer.subarray(0, expected));
      }
    }

    socket.on("data", onData);
    socket.on("error", onError);
    socket.on("end", onEnd);
  });
}

async function send(endpoint, noun) {
  const socket = endpoint.path
    ? net.createConnection(endpoint.path)
    : net.createConnection({ host: "127.0.0.1", port: endpoint.port });
  await new Promise((resolve, reject) => {
    socket.once("connect", resolve);
    socket.once("error", reject);
  });
  socket.write(newtEncode(noun));
  const frame = await readFrame(socket);
  socket.end();
  return newtDecode(frame);
}

function sh(command) {
  return childProcess.execSync(command, { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"] });
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function setupAdbForward(port) {
  const status = sh("adb shell content call --uri content://io.nativeplanet.controller --method getStatus");
  const statusJson = JSON.parse(extractBundleJson(status));
  const pierPath = statusJson.bootPackage && statusJson.bootPackage.pierPath;
  if (!pierPath) {
    throw new Error("controller status has no bootPackage.pierPath");
  }
  const sockPath = `${pierPath}/.urb/conn.sock`;
  sh(`adb forward tcp:${port} localfilesystem:${shellQuote(sockPath)}`);
  return sockPath;
}

function extractBundleJson(text) {
  const marker = "json=";
  const start = text.indexOf(marker);
  if (start < 0) {
    throw new Error("could not find json= in controller status");
  }

  const jsonStart = text.indexOf("{", start + marker.length);
  if (jsonStart < 0) {
    throw new Error("could not find JSON object in controller status");
  }

  let depth = 0;
  let inString = false;
  let escaped = false;
  for (let i = jsonStart; i < text.length; i++) {
    const ch = text[i];
    if (inString) {
      if (escaped) {
        escaped = false;
      } else if (ch === "\\") {
        escaped = true;
      } else if (ch === "\"") {
        inString = false;
      }
      continue;
    }

    if (ch === "\"") {
      inString = true;
    } else if (ch === "{") {
      depth++;
    } else if (ch === "}") {
      depth--;
      if (depth === 0) {
        return text.slice(jsonStart, i + 1);
      }
    }
  }

  throw new Error("unterminated JSON object in controller status");
}

function usage() {
  console.error(`Usage:
  node tools/conn-client.js [--adb] [--port 12321] [--socket /path/to/conn.sock] peel live|who|v|info
  node tools/conn-client.js [--adb] [--port 12321] [--socket /path/to/conn.sock] eval '<hoon>'
  node tools/conn-client.js [--adb] [--port 12321] [--socket /path/to/conn.sock] mobile-apps

Examples:
  node tools/conn-client.js --adb peel live
  node tools/conn-client.js --socket /tmp/nativeplanet-pill-zod/.urb/conn.sock mobile-apps
  node tools/conn-client.js --adb mobile-apps
`);
}

async function main() {
  const args = process.argv.slice(2);
  let port = DEFAULT_PORT;
  let useAdb = false;
  let socketPath = null;

  while (args.length > 0) {
    if (args[0] === "--adb") {
      useAdb = true;
      args.shift();
    } else if (args[0] === "--port") {
      args.shift();
      port = Number(args.shift());
    } else if (args[0] === "--socket") {
      args.shift();
      socketPath = args.shift();
    } else {
      break;
    }
  }

  if (args.length < 1) {
    usage();
    process.exit(2);
  }

  if (useAdb && socketPath) {
    throw new Error("--adb and --socket are mutually exclusive");
  }

  if (useAdb) {
    const sockPath = setupAdbForward(port);
    console.error(`forwarded localhost:${port} -> ${sockPath}`);
  }

  const command = args.shift();
  const endpoint = socketPath ? { path: socketPath } : { port };
  let request;
  if (command === "peel") {
    const peel = args.shift();
    if (!peel) {
      usage();
      process.exit(2);
    }
    request = buildPeel(1n, peel);
    const response = await send(endpoint, request);
    const parsed = parsePeel(response);
    console.log(JSON.stringify({
      requestId: parsed.rid,
      hasValue: parsed.hasValue,
      value: parsed.value ? pretty(parsed.value) : null,
    }, null, 2));
    return;
  }

  if (command === "mobile-apps") {
    request = buildKhanEval(
      1n,
      "=/  m  (strand ,vase)  ;<  x=json  bind:m  (scry json /gx/nativeplanet-mobile/apps/json)  (pure:m !>((en:json:html x)))",
    );
    const response = await send(endpoint, request);
    const value = parseFyrdNounValue(response);
    if (!isAtom(value)) {
      throw new Error(`mobile apps response is not a cord: ${pretty(value)}`);
    }
    console.log(value.toCord());
    return;
  }

  if (command === "eval") {
    const hoon = args.join(" ");
    if (!hoon) {
      usage();
      process.exit(2);
    }
    request = buildKhanEval(1n, hoon);
    const response = await send(endpoint, request);
    console.log(pretty(response));
    return;
  }

  usage();
  process.exit(2);
}

main().catch((error) => {
  console.error(`conn-client: ${error.message}`);
  process.exit(1);
});

# NounCodec Test Fixtures

Test vectors derived from working /tmp/conn-test/conn-client.js implementation.

## Request Fixtures

### %peel %live request
```
Noun: [1 %peel %live ~]
      = Cell(Atom(1), Cell(Atom("peel"), Cell(Atom("live"), Atom(0))))

Jammed BigInteger (hex): 71c087b2b2b63207cb7b2b (approx, may vary by jam cache)

Jammed bytes (little-endian): varies
```

### %peel %who request
```
Noun: [1 %peel %who ~]
      = Cell(Atom(1), Cell(Atom("peel"), Cell(Atom("who"), Atom(0))))
```

### %peel %v request
```
Noun: [1 %peel %v ~]
      = Cell(Atom(1), Cell(Atom("peel"), Cell(Atom(118), Atom(0))))
Note: %v = 118 decimal (ASCII 'v')
```

## Response Fixtures

### %peel %live response (alive)
```
Response noun: [1 0 0]
             = Cell(Atom(1), Cell(Atom(0), Atom(0)))

Interpretation:
- request-id: 1
- result: [~ %.y] where ~ = 0, %.y = 0
- Ship is alive (%.y = loobean true = 0)
```

### %peel %who response
```
Response noun: [1 0 0xf32ff5704979e2ce]
             = Cell(Atom(1), Cell(Atom(0), Atom(0xf32ff5704979e2ce)))

Interpretation:
- request-id: 1
- result: [~ @p] where @p is the ship identity
- For ~namfeb-rossyp-palrum-roclur, @p = 0xf32ff5704979e2ce
```

### %peel %v response
```
Response noun: [1 0 '4.3-33293b1']
             = Cell(Atom(1), Cell(Atom(0), Atom("4.3-33293b1")))

Interpretation:
- request-id: 1
- result: [~ @t] where @t is version cord
```

## Cord Encoding

### Atom("peel")
```
String: "peel"
Bytes (UTF-8): [0x70, 0x65, 0x65, 0x6c] = [112, 101, 101, 108]
Little-endian BigInteger: 0x6c656570 = 1818586480
```

### Atom("live")
```
String: "live"
Bytes (UTF-8): [0x6c, 0x69, 0x76, 0x65] = [108, 105, 118, 101]
Little-endian BigInteger: 0x6576696c = 1702257004
```

### Atom("who")
```
String: "who"
Bytes (UTF-8): [0x77, 0x68, 0x6f] = [119, 104, 111]
Little-endian BigInteger: 0x6f6877 = 7301239
```

## Newt Framing

Format: [0x00][4-byte LE length][jammed payload]

Example for 12-byte payload:
```
Header: 0x00 0x0c 0x00 0x00 0x00
        ^version ^length (12 in LE)
```

## Validation Checklist

1. Atom.fromCord("peel").value == 1818586480
2. Atom.fromCord("live").value == 1702257004
3. Atom.fromCord("who").value == 7301239
4. new Atom(1702257004).toCord() == "live"
5. NounCodec.parseLiveResponse([1 0 0]) == true
6. NounCodec.parseLiveResponse([1 0 1]) == false (%.n = 1)
7. Newt encode/decode round-trip preserves data

## Error Code Reference

ConnSockClient.StatusResult error codes:

| Code | Meaning | When |
|------|---------|------|
| SOCK_MISSING | Socket file doesn't exist | conn.sock not created yet |
| CONN_REFUSED | Connection refused | vere not listening |
| TIMEOUT | Read timeout | vere not responding |
| PERMISSION_DENIED | SELinux/permission error | Missing sock_file rule |
| MALFORMED_FRAME | Invalid newt frame | Protocol corruption |
| CUE_FAILED | Cue decode failed | Invalid jammed data |
| IO_ERROR | Generic I/O error | Other socket errors |
| UNKNOWN | Unclassified error | Unexpected exception |

## Edge Cases

### Null/Empty Handling
- Atom(0) encodes as single bit (1) in jam
- Empty cord ("") -> Atom(0)
- Atom(0).toCord() -> ""

### Back-reference Limits
- rub() limits leading zeros to 64 to prevent DoS
- Back-references must point to previously seen position

### Response Format Validation
- %peel response: [rid [~ value]] for some, [rid ~] for none
- %.y (true) = 0, %.n (false) = 1 (loobeans are inverted)
- @p values are raw BigInteger, not @p-formatted strings

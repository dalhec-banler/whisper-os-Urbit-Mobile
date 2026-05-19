# Whisper OS Onboarding

## User Journey

### Path A: Fresh Start (New User)

1. **Install Whisper OS ROM**
   - Flash factory image to supported Pixel device
   - Complete Android setup wizard

2. **Launch Whisper App**
   - App prompts for identity choice
   - Options: Generate new comet, Import planet, Scan BootPackage

3. **Generate Comet (Default)**
   - Automatic comet generation
   - No external dependencies
   - Ready in ~90 seconds

4. **First Run**
   - Landscape UI loads in WebView
   - Basic tutorial overlay
   - Invite to connect with parent (optional)

### Path B: Import from Desktop

1. **On Desktop Urbit**
   - Generate BootPackage for moon/comet
   - Display QR code or export file

2. **On Whisper Device**
   - Scan QR or import file
   - Enter encryption passphrase
   - Boot from BootPackage

3. **Initial Sync**
   - Connect to parent planet
   - Sync recent messages
   - Download essential desk updates

### Path C: Existing Planet Owner

1. **Export Keys**
   - Export planet keys from current host
   - Create encrypted key bundle

2. **Import to Whisper**
   - Import key bundle
   - Provide passphrase
   - Planet boots on device

3. **Considerations**
   - Cannot run same planet in two places
   - Recommended: Use moon instead

## Onboarding Screens

### Screen 1: Welcome
```
┌─────────────────────────────┐
│                             │
│     [Whisper OS Logo]       │
│                             │
│   Your sovereign phone      │
│                             │
│     [Get Started →]         │
│                             │
└─────────────────────────────┘
```

### Screen 2: Identity Choice
```
┌─────────────────────────────┐
│   How do you want to start? │
│                             │
│   ┌─────────────────────┐   │
│   │ 🌟 New Identity     │   │
│   │ Generate a comet    │   │
│   └─────────────────────┘   │
│                             │
│   ┌─────────────────────┐   │
│   │ 📱 Import Package   │   │
│   │ Scan QR or file     │   │
│   └─────────────────────┘   │
│                             │
│   ┌─────────────────────┐   │
│   │ 🔑 Import Planet    │   │
│   │ Advanced users      │   │
│   └─────────────────────┘   │
│                             │
└─────────────────────────────┘
```

### Screen 3: Booting
```
┌─────────────────────────────┐
│                             │
│     Booting your urbit...   │
│                             │
│     [Progress animation]    │
│                             │
│     ~sampel-palnet          │
│                             │
│     This takes about        │
│     90 seconds on first run │
│                             │
└─────────────────────────────┘
```

### Screen 4: Ready
```
┌─────────────────────────────┐
│                             │
│     ✓ Ready to go!          │
│                             │
│     Your identity:          │
│     ~sampel-palnet          │
│                             │
│     [Open Landscape →]      │
│                             │
│     [Connect to Parent]     │
│     (optional)              │
│                             │
└─────────────────────────────┘
```

## Technical Requirements

### First Boot Time Budget
- Pill load: 5s
- Kernel compile: 60s
- Desk setup: 20s
- Total: ~90s

### Subsequent Boot
- Pier load: 5s
- Ready: 10s

### Storage Requirements
- Fresh comet: ~500MB
- After sync: 1-2GB typical
- Maximum pier: Configurable

## Error Handling

### Boot Failure
- Retry with fresh pier
- Option to clear and restart
- Link to troubleshooting docs

### Sync Failure
- Continue in offline mode
- Retry sync later
- Show clear status indicator

### Import Failure
- Validate package format
- Check passphrase
- Offer manual recovery

## Metrics to Track

- Time to first message sent
- Onboarding completion rate
- Path selection distribution
- Error frequency by type

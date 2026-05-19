# SELinux Policy for NativePlanet Vere

## Overview

The vere runtime runs in a dedicated SELinux domain `nativeplanet_vere` with minimal permissions required for urbit operation.

## Domain Definition

```te
# nativeplanet_vere.te

type nativeplanet_vere, domain, coredomain;
type nativeplanet_vere_exec, exec_type, system_file_type, file_type;

init_daemon_domain(nativeplanet_vere)
net_domain(nativeplanet_vere)
io_uring_use(nativeplanet_vere)
```

## File Contexts

```
# file_contexts

/data/nativeplanet(/.*)?                 u:object_r:nativeplanet_data_file:s0
/system_ext/bin/vere                     u:object_r:nativeplanet_vere_exec:s0
/system_ext/bin/nativeplanet-vere-launch u:object_r:nativeplanet_vere_exec:s0
```

## Permissions

### Data Directory Access

```te
allow nativeplanet_vere nativeplanet_data_file:dir create_dir_perms;
allow nativeplanet_vere nativeplanet_data_file:file create_file_perms;
allow nativeplanet_vere nativeplanet_data_file:lnk_file create_file_perms;
```

### System File Access

```te
allow nativeplanet_vere system_file:dir r_dir_perms;
allow nativeplanet_vere system_file:file rx_file_perms;
```

### Self-Execution (fork/exec)

```te
allow nativeplanet_vere nativeplanet_vere_exec:file { execute execute_no_trans };
```

### Special ioctls

```te
# TIOCGWINSZ for terminal handling
allowxperm nativeplanet_vere nativeplanet_data_file:file ioctl { 0x5413 };
```

## Property Contexts

```
# property_contexts

nativeplanet.vere.enabled   u:object_r:nativeplanet_property:s0
```

## Property Rules

```te
# nativeplanet_property.te

type nativeplanet_property, property_type;

set_prop(shell, nativeplanet_property)
get_prop(init, nativeplanet_property)
```

## Known Harmless Denials

The following denial appears in logs but does not affect operation:

```
avc: denied { write } for path="/dev/kmsg_debug" ... tclass=chr_file
```

This is vere attempting to write to kernel log, which is not needed for operation.

## Adding New Permissions

If vere needs new capabilities:

1. Identify the denial in `dmesg | grep avc`
2. Add minimal permission to `nativeplanet_vere.te`
3. Rebuild: `m selinux_policy`
4. Test thoroughly

## Security Considerations

- Domain is `coredomain` for access to system resources
- Network access via `net_domain` macro
- No root capabilities beyond init transition
- Data isolated in `/data/nativeplanet/`

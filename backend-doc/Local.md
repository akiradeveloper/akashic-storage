This is the default backend. With this backend, all data are stored in the local filesystem
(or network filesystem on some mount point).

It simply requires mountpoint.

```
backend {
  type = akashic.storage.backend.impl.Local
  # The mountpoint of the backing filesystem
  # User need to prepare this mountpoint before starting akashic-storage server.
  mountpoint = /mnt/akashic-storage
}
```

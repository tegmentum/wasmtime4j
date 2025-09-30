package ai.tegmentum.wasmtime4j.wasi;

/**
 * Implementation of WasiPermissions for file and directory permissions.
 *
 * @since 1.0.0
 */
final class WasiPermissionsImpl implements WasiPermissions {

  private final int mode;

  WasiPermissionsImpl(final int mode) {
    if (mode < 0 || mode > 07777) {
      throw new IllegalArgumentException("Invalid permission mode: " + Integer.toOctalString(mode));
    }
    this.mode = mode;
  }

  @Override
  public int getMode() {
    return mode;
  }

  @Override
  public boolean isOwnerRead() {
    return (mode & 0400) != 0;
  }

  @Override
  public boolean isOwnerWrite() {
    return (mode & 0200) != 0;
  }

  @Override
  public boolean isOwnerExecute() {
    return (mode & 0100) != 0;
  }

  @Override
  public boolean isGroupRead() {
    return (mode & 0040) != 0;
  }

  @Override
  public boolean isGroupWrite() {
    return (mode & 0020) != 0;
  }

  @Override
  public boolean isGroupExecute() {
    return (mode & 0010) != 0;
  }

  @Override
  public boolean isOtherRead() {
    return (mode & 0004) != 0;
  }

  @Override
  public boolean isOtherWrite() {
    return (mode & 0002) != 0;
  }

  @Override
  public boolean isOtherExecute() {
    return (mode & 0001) != 0;
  }

  @Override
  public boolean isSetuid() {
    return (mode & 04000) != 0;
  }

  @Override
  public boolean isSetgid() {
    return (mode & 02000) != 0;
  }

  @Override
  public boolean isSticky() {
    return (mode & 01000) != 0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final WasiPermissionsImpl that = (WasiPermissionsImpl) obj;
    return mode == that.mode;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(mode);
  }

  @Override
  public String toString() {
    return "WasiPermissions{mode=" + Integer.toOctalString(mode) + "}";
  }

  /** Builder implementation for WasiPermissions. */
  static final class BuilderImpl implements WasiPermissions.Builder {
    private int mode = 0;

    @Override
    public Builder ownerRead(final boolean read) {
      if (read) {
        mode |= 0400;
      } else {
        mode &= ~0400;
      }
      return this;
    }

    @Override
    public Builder ownerWrite(final boolean write) {
      if (write) {
        mode |= 0200;
      } else {
        mode &= ~0200;
      }
      return this;
    }

    @Override
    public Builder ownerExecute(final boolean execute) {
      if (execute) {
        mode |= 0100;
      } else {
        mode &= ~0100;
      }
      return this;
    }

    @Override
    public Builder groupRead(final boolean read) {
      if (read) {
        mode |= 0040;
      } else {
        mode &= ~0040;
      }
      return this;
    }

    @Override
    public Builder groupWrite(final boolean write) {
      if (write) {
        mode |= 0020;
      } else {
        mode &= ~0020;
      }
      return this;
    }

    @Override
    public Builder groupExecute(final boolean execute) {
      if (execute) {
        mode |= 0010;
      } else {
        mode &= ~0010;
      }
      return this;
    }

    @Override
    public Builder otherRead(final boolean read) {
      if (read) {
        mode |= 0004;
      } else {
        mode &= ~0004;
      }
      return this;
    }

    @Override
    public Builder otherWrite(final boolean write) {
      if (write) {
        mode |= 0002;
      } else {
        mode &= ~0002;
      }
      return this;
    }

    @Override
    public Builder otherExecute(final boolean execute) {
      if (execute) {
        mode |= 0001;
      } else {
        mode &= ~0001;
      }
      return this;
    }

    @Override
    public Builder setuid(final boolean setuid) {
      if (setuid) {
        mode |= 04000;
      } else {
        mode &= ~04000;
      }
      return this;
    }

    @Override
    public Builder setgid(final boolean setgid) {
      if (setgid) {
        mode |= 02000;
      } else {
        mode &= ~02000;
      }
      return this;
    }

    @Override
    public Builder sticky(final boolean sticky) {
      if (sticky) {
        mode |= 01000;
      } else {
        mode &= ~01000;
      }
      return this;
    }

    @Override
    public Builder mode(final int mode) {
      if (mode < 0 || mode > 07777) {
        throw new IllegalArgumentException(
            "Invalid permission mode: " + Integer.toOctalString(mode));
      }
      this.mode = mode;
      return this;
    }

    @Override
    public WasiPermissions build() {
      return new WasiPermissionsImpl(mode);
    }
  }
}

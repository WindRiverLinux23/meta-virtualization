include runc.inc

SRCREV = "0f48801a0e21e3f0bc4e74643ead2a502df4818d"
SRC_URI = " \
    git://github.com/opencontainers/runc;branch=release-1.1;protocol=https \
    file://0001-Makefile-respect-GOBUILDFLAGS-for-runc-and-remove-re.patch \
    "
RUNC_VERSION = "1.1.6"

CVE_PRODUCT = "runc"

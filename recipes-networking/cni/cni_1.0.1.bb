HOMEPAGE = "https://github.com/containernetworking/cni"
SUMMARY = "Container Network Interface - networking for Linux containers"
DESCRIPTION = "CNI (Container Network Interface), a Cloud Native Computing \
Foundation project, consists of a specification and libraries for writing \
plugins to configure network interfaces in Linux containers, along with a \
number of supported plugins. CNI concerns itself only with network connectivity \
of containers and removing allocated resources when the container is deleted. \
Because of this focus, CNI has a wide range of support and the specification \
is simple to implement. \
"

# 1.0.1
SRCREV_cni = "1694fd7b57e0176a98a12823a5ffc03337fdc152"
# 1.1.1
SRCREV_plugins = "4744ec27b89c083194e7df498de50f03a8a1d3ec"
SRCREV_flannel_plugin = "076c4462d6c6887614fc881b806b690b9e56ceb2"
SRC_URI = "\
	git://github.com/containernetworking/cni.git;branch=main;name=cni;protocol=https \
        git://github.com/containernetworking/plugins.git;branch=release-1.1;destsuffix=plugins;name=plugins;protocol=https \
        git://github.com/flannel-io/cni-plugin;branch=main;name=flannel_plugin;protocol=https;destsuffix=plugins/plugins/meta/flannel \
	"

RPROVIDES:${PN} += "kubernetes-cni"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://src/import/LICENSE;md5=fa818a259cbed7ce8bc2a22d35a464fc"

GO_IMPORT = "import"

PV = "1.0.1"

COMPATIBLE_HOST:mips = "null"

inherit go
inherit goarch

do_compile() {
	# link fixups for compilation
	rm -f ${S}/src/import/vendor/src
	mkdir -p ${S}/src/import/vendor/
	ln -sf ./ ${S}/src/import/vendor/src
	rm -rf ${S}/src/import/plugins
	rm -rf ${S}/src/import/vendor/github.com/containernetworking/plugins

	mkdir -p ${S}/src/import/vendor/github.com/containernetworking/cni

	ln -sf ../../../../libcni ${S}/src/import/vendor/github.com/containernetworking/cni/libcni
	ln -sf ../../../../pkg ${S}/src/import/vendor/github.com/containernetworking/cni/pkg
	ln -sf ../../../../cnitool ${S}/src/import/vendor/github.com/containernetworking/cni/cnitool
	ln -sf ${WORKDIR}/plugins ${S}/src/import/vendor/github.com/containernetworking/plugins

	export GO111MODULE="off"
	export GOPATH="${S}/src/import/.gopath:${S}/src/import/vendor:${STAGING_DIR_TARGET}/${prefix}/local/go"
	export CGO_ENABLED="1"

	cd ${S}/src/import/vendor/github.com/containernetworking/cni/libcni
	${GO} build

	cd ${S}/src/import/vendor/github.com/containernetworking/cni/cnitool
	${GO} build

	cd ${S}/src/import/vendor/github.com/containernetworking/plugins/
	PLUGINS="$(ls -d plugins/meta/*; ls -d plugins/ipam/*; ls -d plugins/main/* | grep -v windows)"
	mkdir -p ${WORKDIR}/plugins/bin/
	for p in $PLUGINS; do
	    plugin="$(basename "$p")"
	    echo "building: $p"
	    ${GO} build -o ${WORKDIR}/plugins/bin/$plugin -ldflags '-extldflags -static -X github.com/containernetworking/plugins/pkg/utils/buildversion.BuildVersion=v1.1.1' github.com/containernetworking/plugins/$p
	done
}

do_install() {
    localbindir="${libexecdir}/cni/"

    install -d ${D}${localbindir}
    install -d ${D}/${sysconfdir}/cni/net.d

    install -m 755 ${S}/src/import/cnitool/cnitool ${D}/${localbindir}
    install -m 755 -D ${WORKDIR}/plugins/bin/* ${D}/${localbindir}

    # Parts of k8s expect the cni binaries to be available in /opt/cni
    install -d ${D}/opt/cni
    ln -sf ${libexecdir}/cni/ ${D}/opt/cni/bin
}

FILES:${PN} += "${libexecdir}/cni/* /opt/cni/bin"

INSANE_SKIP:${PN} += "ldflags already-stripped"

RDEPENDS:${PN} += " ca-certificates"

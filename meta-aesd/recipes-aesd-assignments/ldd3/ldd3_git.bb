# See https://git.yoctoproject.org/poky/tree/meta/files/common-licenses
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit module
# This sets your staging directory based on WORKDIR, where WORKDIR is defined at 
# https://docs.yoctoproject.org/ref-manual/variables.html?highlight=workdir#term-WORKDIR
# We reference the "server" directory here to build from the "server" directory
# in your assignments repo
SRC_URI = "git://git@github.com/cu-ecen-aeld/assignment-7-KareemLMR;protocol=ssh;branch=main"

PV = "1.0+git${SRCPV}"
# TODO: set to reference a specific commit hash in your assignment repo
SRCREV = "3e805aa82978793201e58a4b8cdb28dc3583ce38"

S = "${WORKDIR}/git"
# TODO: Add the aesdsocket application and any other files you need to install
# See https://git.yoctoproject.org/poky/plain/meta/conf/bitbake.conf?h=kirkstone
INITDIR="${sysconfdir}/init.d"
FILES:${PN} += "\
                      ${INITDIR}/scull_unload \
                      ${INITDIR}/scull_load \
                      ${INITDIR}/module_unload \
                      ${INITDIR}/module_load \
                      ${INITDIR}/S98lddmodules \
                      ${sysconfdir}/rc5.d/S98lddmodules \
               "

INSANE_SKIP:${PN} += "ldflags"
# TODO: customize these as necessary for any libraries you need for your application
# (and remove comment)
TARGET_LDFLAGS += "-pthread -lrt"

do_configure () {
	:
}

export EXTRA_CFLAGS = "${CFLAGS}"
export EXTRA_LDFLAGS = "${LDFLAGS}"

EXTRA_OEMAKE = "CC='${CC}' LD='${CCLD}' V=1 ARCH=${TARGET_ARCH} CROSS_COMPILE=${TARGET_PREFIX} SKIP_STRIP=y HOSTCC='${BUILD_CC}' HOSTCPP='${BUILD_CPP}'"

KERNEL_SRC ?= "${STAGING_KERNEL_DIR}"

do_compile () {
        cd ${S}/scull
	make -C ${KERNEL_SRC} M=${S}/scull modules
        cd ${S}/misc-modules
        make -C ${KERNEL_SRC} M=${S}/misc-modules modules
}

do_install() {
    install -d ${D}/lib/modules/${KERNEL_VERSION}/extra
    install -m 0644 ${S}/scull/scull.ko ${D}/lib/modules/${KERNEL_VERSION}/extra
    install -m 0644 ${S}/misc-modules/hello.ko ${D}/lib/modules/${KERNEL_VERSION}/extra
    install -m 0644 ${S}/misc-modules/faulty.ko ${D}/lib/modules/${KERNEL_VERSION}/extra
    
    install -d ${D}${INITDIR}
    install -m 0755 ${S}/base_external/rootfs_overlay/etc/init.d/module_load ${D}${INITDIR}/module_load
    install -m 0755 ${S}/base_external/rootfs_overlay/etc/init.d/module_unload ${D}${INITDIR}/module_unload
    install -m 0755 ${S}/base_external/rootfs_overlay/etc/init.d/scull_load ${D}${INITDIR}/scull_load
    install -m 0755 ${S}/base_external/rootfs_overlay/etc/init.d/scull_unload ${D}${INITDIR}/scull_unload
    install -m 0755 ${S}/base_external/rootfs_overlay/etc/init.d/S98lddmodules ${D}${INITDIR}/S98lddmodules

    install -d ${D}${sysconfdir}/rc5.d
    ln -sf ../init.d/S98lddmodules ${D}${sysconfdir}/rc5.d/S98lddmodules
}

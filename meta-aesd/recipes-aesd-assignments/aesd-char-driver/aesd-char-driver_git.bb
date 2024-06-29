# See https://git.yoctoproject.org/poky/tree/meta/files/common-licenses
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit module
# This sets your staging directory based on WORKDIR, where WORKDIR is defined at 
# https://docs.yoctoproject.org/ref-manual/variables.html?highlight=workdir#term-WORKDIR
# We reference the "server" directory here to build from the "server" directory
# in your assignments repo
SRC_URI = "git://git@github.com/cu-ecen-aeld/assignments-3-and-later-KareemLMR;protocol=ssh;branch=main"
PV = "1.0+git${SRCPV}"
# TODO: set to reference a specific commit hash in your assignment repo
SRCREV = "3e2f8c13ea1c2ada32c156b9763f46dda153a602"

S = "${WORKDIR}/git/aesd-char-driver"
# TODO: Add the aesdsocket application and any other files you need to install
# See https://git.yoctoproject.org/poky/plain/meta/conf/bitbake.conf?h=kirkstone
INITDIR="${sysconfdir}/init.d"
FILES:${PN} += "\
                   ${INITDIR}/aesdchar_load \
                   ${INITDIR}/aesdchar_unload \
                   ${INITDIR}/S98aesdcharmodule \
                   ${sysconfdir}/rc5.d/S98aesdcharmodule \
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
	make -C ${KERNEL_SRC} M=${S} modules
}

do_install() {
    install -d ${D}/lib/modules/${KERNEL_VERSION}/extra
    install -m 0644 ${S}/aesdchar.ko ${D}/lib/modules/${KERNEL_VERSION}/extra
    
    install -d ${D}${INITDIR}
    install -m 0755 ${S}/aesdchar_load ${D}${INITDIR}/aesdchar_load
    install -m 0755 ${S}/aesdchar_unload ${D}${INITDIR}/aesdchar_unload
    install -m 0755 ${S}/S98aesdcharmodule ${D}${INITDIR}/S98aesdcharmodule

    install -d ${D}${sysconfdir}/rc5.d
    ln -sf ../init.d/S98aesdcharmodule ${D}${sysconfdir}/rc5.d/S98aesdcharmodule
}

# KylGis POS — Authors and Contributions

KylGis POS is a derivative free-software project. Rebranding, package migration, or later modifications do not erase the authorship of upstream code.

## Upstream lineage

The project contains code and/or derived portions originating from Openbravo POS, uniCenta oPOS, Chromis POS / ChromisKitchenScreen, and other third-party projects. Copyright and author notices present in individual files must be preserved where applicable.

Important upstream contributors visible in the source history include Adrián Romero / Openbravo S.L., Hugh Clayson / uniCenta, John Lewis / Chromis, Jack Gerrard and other authors identified in individual source files.

## KylGis POS development

KylGis POS modifications and new functionality in the 2026 fork are developed under the KylGis POS project. Git history records Fidel Arcos as the principal author of the current KylGis functional commits.

Identified KylGis contributions include: independent application launchers and multi-instance control; configurable ticket widths and paper formats; digital JPG receipts; mirrored physical/screen/digital printing; preview isolation; platform-order workflow; configurable sales buttons; KitchenScreen/remote-order reliability improvements; native replacement of the old SendOrder BeanShell flow; ESC/POS logo/raster corrections; RXTX port-release fixes; catalog/navigation fixes; and removal of obsolete posApps integration.

## Derived components with specific provenance

### ImporteALetras

The Spanish number-to-words core is derived from Openbravo `NumberToWord_es`. POS/MXN adaptations were publicly documented by Fidel Arcos Mota in the uniCenta community in 2020. KylGis POS 2026 ports that functionality to compiled Java and adds large-value/overflow handling, receipt-oriented absolute-value formatting, rounding, capitalization, and word-safe multi-line formatting.

Openbravo/Etendo technical reference:
https://docs.etendo.software/developer-guide/etendo-classic/bundles/platform/overview/

Public 2020 uniCenta contribution:
https://sourceforge.net/p/unicentaopos/discussion/1126900/thread/c43fcdb599/

### RemoteOrderPrinter / SendOrder

The native Java remote-printer dispatcher in `JPanelTicket` is a refactor/replacement of the GPL `script.SendOrder` behavior inherited from uniCenta. KylGis POS adds native execution without BeanShell, pending-line selection, duplicate-click prevention, reliable button state, batch `COMPLETETIME`, auxiliary/component metadata and safer KitchenScreen order synchronization.

### KitchenScreen integration

The original remote-order/KitchenScreen infrastructure is inherited. KylGis POS credits apply to the later integration fixes and extensions, not to the existence of the original connector or order schema.

### Ticket templates

Current ticket XML templates are substantially adapted from inherited templates. They remain derivative works even when moved to a new path; KylGis POS claims the 2026 modifications, not exclusive authorship of the original template design.

## License

KylGis POS is distributed under the GNU General Public License terms applicable to the project. Upstream and third-party notices and licenses remain applicable to their respective portions.

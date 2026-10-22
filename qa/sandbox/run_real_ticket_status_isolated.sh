#!/usr/bin/env bash
# Entirely synthetic MariaDB, real CORE1.3 Session and DataLogicSales.saveTicket.
set -Eeuo pipefail
version="${KYLGIS_QA_MARIADB_VERSION:-10.11}"
case "$version" in 10.11|11.8|12.3.3) ;; *) echo REFUSE_IMAGE;exit 2;; esac
image="docker.io/library/mariadb:$version"
name="kylgis-leader-status-${version//./_}-20261021"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
jdk="/usr/lib/jvm/temurin-21-jdk"
sealed="/home/fam/00-MultiPlataforma/PendientesDeRevision/Java21_MonitorCocina/entradas_sello/kylgispos_CORE13_ecc1047d.jar"
mysql="/home/fam/.m2/repository/mysql/mysql-connector-java/5.1.39/mysql-connector-java-5.1.39.jar"
commons="/home/fam/.m2/repository/commons-lang/commons-lang/2.6/commons-lang-2.6.jar"
expected="ecc1047d67df07b7178b2bad27eec9d931332bb43285e9a840da3835d86fce2b"
owned=0;tmp=""
cleanup() {
 local rc=$?; trap - EXIT
 if [[ "$owned" == 1 ]]; then
  if ((rc!=0)); then podman logs --tail 16 "$name" >&2 || true;fi
  podman stop -t 5 "$name" >/dev/null 2>&1 || true
  if podman container exists "$name";then echo QA_CONTAINER_LEFT_RUNNING >&2;rc=90
  else echo QA_STATUS_CONTAINER_CLEANED;fi
 fi
 if [[ -n "$tmp" && "$tmp" == /tmp/kylgis-status-save.* ]]; then rm -rf -- "$tmp";fi
 exit "$rc"
}
trap cleanup EXIT
[[ -x "$jdk/bin/java" && -x "$jdk/bin/javac" ]] || { echo JDK21_MISSING;exit 2; }
[[ -f "$sealed" && -f "$mysql" && -f "$commons" ]] || { echo QA_DEPENDENCY_MISSING;exit 2; }
[[ "$(sha256sum "$sealed" | cut -d' ' -f1)" == "$expected" ]] || { echo REFUSE_CORE_SHA;exit 3; }
podman image exists "$image" || { echo NO_LOCAL_IMAGE;exit 2; }
if podman container exists "$name";then echo REFUSE_EXISTING_CONTAINER;exit 3;fi
tmp="$(mktemp -d /tmp/kylgis-status-save.XXXXXX)"
mkdir -p "$tmp/classes"
"$jdk/bin/javac" --release 21 -cp "$sealed:$mysql:$commons" -d "$tmp/classes" \
 "$root/src/main/java/com/mx/kylgis/pos/ticket/TicketInfo.java" \
 "$root/src/main/java/com/mx/kylgis/pos/forms/DataLogicSales.java" \
 "$root/src/main/java/com/mx/kylgis/pos/forms/SaleOperationLedgerQA.java" \
 "$root/src/main/java/com/mx/kylgis/pos/data/loader/Session.java" \
 "$root/src/main/java/com/mx/kylgis/pos/data/loader/Transaction.java" \
 "$root/src/main/java/com/mx/kylgis/pos/data/loader/TransactionOutcomeUnknownException.java" \
 "$root/qa/manual/TicketStatusSchemaProbe.java" \
 "$root/qa/manual/RealTicketStatusSaveProbe.java" \
 "$root/qa/manual/RealVoucherDiscountRaceProbe.java" \
 "$root/qa/manual/RealSaveTicketAckLossProbe.java" \
 "$root/qa/manual/UnknownCommitFlagSerializationProbe.java" \
 "$root/qa/manual/TicketStatusSetterProbe.java"
echo STATUS_QA_COMPILED_REAL_CORE_CLASSES
podman run -d --rm --name "$name" --network none --read-only --cpus 2 --memory 1g \
 --tmpfs /var/lib/mysql:rw,size=512m --tmpfs /tmp:rw,size=192m \
 --tmpfs /run/mysqld:rw,size=16m \
 --mount "type=bind,source=$jdk,target=/opt/jdk,readonly=true" \
 --env MARIADB_ALLOW_EMPTY_ROOT_PASSWORD=1 "$image" >/dev/null
owned=1;ready=0
for _ in $(seq 1 100);do
 if podman logs "$name" 2>&1 | grep -F 'MariaDB init process done. Ready for start up.' >/dev/null \
  && podman exec "$name" mariadb -uroot --protocol=SOCKET --skip-ssl -e 'SELECT 1' >/dev/null 2>&1;then ready=1;break;fi
 sleep 1
done
[[ "$ready" == 1 ]] || { echo DB_NOT_READY;exit 4; }
podman exec "$name" mariadb -uroot --protocol=SOCKET --skip-ssl \
 -e 'CREATE DATABASE pos_qa_status CHARACTER SET utf8mb4'
podman exec "$name" mkdir -p /tmp/qa-status-classes /tmp/qa-home
podman cp "$tmp/classes/." "$name:/tmp/qa-status-classes"
podman cp "$sealed" "$name:/tmp/qa-core13.jar"
podman cp "$mysql" "$name:/tmp/qa-mysql.jar"
podman cp "$commons" "$name:/tmp/qa-commons-lang.jar"
cpvalue="/tmp/qa-status-classes:/tmp/qa-core13.jar:/tmp/qa-mysql.jar:/tmp/qa-commons-lang.jar"
echo LOCAL_UNKNOWN_COMMIT_FLAG_SERIALIZATION_START
podman exec "$name" /opt/jdk/bin/java -Xmx128m -Duser.home=/tmp/qa-home \
 -cp "$cpvalue" UnknownCommitFlagSerializationProbe
echo "REAL_CORE_SCHEMA_QA_START mariadb=$version"
podman exec "$name" /opt/jdk/bin/java -Xmx192m -Duser.home=/tmp/qa-home \
 -cp "$cpvalue" TicketStatusSchemaProbe
echo REAL_SAVE_TICKET_STATUS_START
ledger_opt=()
if [[ "${KYLGIS_QA_LEDGER_OPTIN:-0}" == 1 ]]; then
  podman exec -i "$name" mariadb -uroot --protocol=SOCKET --skip-ssl pos_qa_status < "$root/qa/sandbox/qa_durable_payment_ledger_candidate.sql"
  ledger_opt=(-Dkylgis.qa.durableLedger.enabled=true)
  echo QA_LEDGER_OPTIN_SCHEMA_READY
fi
podman exec -e KYLGIS_QA_SCOPE=ONLY_SYNTHETIC_POS_STATUS "$name" \
 /opt/jdk/bin/java -Xmx192m -Duser.home=/tmp/qa-home "${ledger_opt[@]}" \
 -cp "$cpvalue" RealTicketStatusSaveProbe
echo "REAL_SAVE_TICKET_STATUS_QA_PASS mariadb=$version"
echo REAL_VOUCHER_DISCOUNT_RACE_START
podman exec -e KYLGIS_QA_SCOPE=ONLY_SYNTHETIC_POS_STATUS "$name" \
 /opt/jdk/bin/java -Xmx192m -Duser.home=/tmp/qa-home \
 -cp "$cpvalue" RealVoucherDiscountRaceProbe
echo "REAL_VOUCHER_RACE_QA_PASS mariadb=$version"
echo REAL_SAVE_TICKET_ACK_LOSS_START
podman exec -e KYLGIS_QA_SCOPE=ONLY_SYNTHETIC_POS_STATUS \
 -e KYLGIS_QA_LEDGER_ACK_OPTIN="${KYLGIS_QA_LEDGER_ACK_OPTIN:-0}" "$name" \
 /opt/jdk/bin/java -Xmx192m -Duser.home=/tmp/qa-home \
 -cp "$cpvalue" RealSaveTicketAckLossProbe
echo "REAL_SAVE_TICKET_ACK_LOSS_QA_PASS mariadb=$version"

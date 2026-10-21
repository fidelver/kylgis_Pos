#!/usr/bin/env bash
# QA-only idempotency DDL; absolutely no POS databases.
set -Eeuo pipefail
version="${KYLGIS_QA_MARIADB_VERSION:-10.11}"
case "$version" in 10.11|11.8|12.3.3) ;; *) echo "REFUSE_VERSION"; exit 2;; esac
image="docker.io/library/mariadb:$version"
name="kylgis-qa-ledger-constraints-${version//./_}"
dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
sql="$dir/qa_durable_payment_ledger_candidate.sql"
owned=0
cleanup(){
 local rc=$?; trap - EXIT
 if (( owned == 1 )); then
   if (( rc != 0 )); then podman logs --tail 25 "$name" >&2 || true; fi
   podman stop -t 3 "$name" >/dev/null 2>&1 || true
   if podman container exists "$name"; then echo "QA_CLEANUP_FAILED"; rc=90; else echo "QA_CLEANED"; fi
 fi
 exit "$rc"
}
trap cleanup EXIT
podman image exists "$image" || { echo "QA_IMAGE_MISSING"; exit 2; }
if podman container exists "$name"; then echo "QA_CONTAINER_NAME_OCCUPIED"; exit 3; fi
podman run -d --rm --name "$name" --network none --read-only \
 --tmpfs /var/lib/mysql:rw,size=512m --tmpfs /tmp:rw,size=128m \
 --tmpfs /run/mysqld:rw,size=8m --memory 1g --cpus 2 \
 -e MARIADB_ALLOW_EMPTY_ROOT_PASSWORD=1 "$image" >/dev/null
owned=1
ready=0
for _ in $(seq 1 90); do
 if podman logs "$name" 2>&1 | grep -F 'MariaDB init process done. Ready for start up.' >/dev/null \
 && podman exec "$name" mariadb -uroot --protocol=SOCKET -e 'SELECT 1' >/dev/null 2>&1; then ready=1;break;fi
 sleep 1
done
[[ "$ready" == 1 ]] || { echo "QA_DB_NOT_READY";exit 4; }
podman exec "$name" mariadb -uroot --protocol=SOCKET -e 'CREATE DATABASE isolated_ledger_qa'
podman exec -i "$name" mariadb -uroot --protocol=SOCKET isolated_ledger_qa < "$sql"
query(){ podman exec "$name" mariadb -N -uroot --protocol=SOCKET isolated_ledger_qa -e "$1"; }
reject(){ if query "$1" >/dev/null 2>&1;then echo "UNSAFE_ACCEPTED:$2";exit 6;fi;echo "PASS_REJECT_$2"; }
query "INSERT INTO qa_sale_operation_intents (operation_uuid,receipt_id,terminal_id) VALUES ('00000000-0000-0000-0000-000000000001','R1','C1')"
reject "INSERT INTO qa_sale_operation_intents (operation_uuid,receipt_id,terminal_id) VALUES ('00000000-0000-0000-0000-000000000002','R1','C2')" "DUP_RECEIPT"
reject "UPDATE qa_sale_operation_intents SET state='INVALID' WHERE receipt_id='R1'" "BAD_STATE"
query "INSERT INTO qa_external_payment_intents(payment_uuid,operation_uuid,provider,provider_reference,amount,currency) VALUES ('10000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001','MOCK','BANK-REF-1',100.00,'MXN')"
reject "INSERT INTO qa_external_payment_intents(payment_uuid,operation_uuid,provider,provider_reference,amount,currency) VALUES ('10000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000001','MOCK','BANK-REF-1',100.00,'MXN')" "DUP_BANK_REF"
reject "INSERT INTO qa_external_payment_intents(payment_uuid,operation_uuid,provider,amount) VALUES ('10000000-0000-0000-0000-000000000003','00000000-0000-0000-0000-000000000099','MOCK',1.00)" "FOREIGN_KEY"
reject "UPDATE qa_external_payment_intents SET state='INVALID' WHERE provider_reference='BANK-REF-1'" "BAD_PAYMENT_STATE"
query "UPDATE qa_sale_operation_intents SET state='COMMIT_UNKNOWN' WHERE receipt_id='R1'"
[[ "$(query "SELECT state FROM qa_sale_operation_intents WHERE receipt_id='R1'")" == "COMMIT_UNKNOWN" ]] || { echo QA_STATE_FAILURE;exit 7; }
echo "QA_LEDGER_CONSTRAINTS_PASS version=$version checks=6"

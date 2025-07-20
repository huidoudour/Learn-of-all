
openssl req -new -key abc.lan.key -out abc.lan.csr -config /etc/pki/tls/openssl.cnf -extensions v3_req

openssl ca -in abc.lan.csr  -out abc.lan.crt -cert ca.crt  -keyfile ca.key -extensions v3_req -days 1825 -config /etc/pki/tls/openssl.cnf

scp linux3:/etc/pki/CA/abc.lan.crt /etc/pki/tls/certs/abc.lan.crt

scp linux3:/etc/pki/CA/abc.lan.key /etc/pki/tls/private/abc.lan.key
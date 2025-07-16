```bash
touch index.txt

echo 00 > serial

openssl genrsa -out ca.key 2048 

openssl req -new -out ca.csr -key ca.key

openssl x509 -req -days 3650 -in ca.csr -signeky ca.key -out ca.crt

/etc/pki/tls/openssl.cnf
{
	dir = /etc/pki/CA
	subjectAltName = @alt_names

	[alt_names]
	DNS.1=xckt.com
	DNS.2=*.xckt.com
}

openssl genrsa -out xckt.key 2048

openssl req -new -out xckt.csr -key xckt.key -config /etc/pki/tls/openssl.cnf -extensions v3_req

openssl ca -in xckt.csr -out xckt.crt -cert ca.crt -keyfile ca.key -extensions v3_req -days 1825 -config /etc/pki/tls/openssl.cnf

```
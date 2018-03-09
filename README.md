# pakrunner

Pakrunner je REST servis za kontrolu i monitoring dugotrajnih proraucna. 

## Primeri poziva

### Pokretanje proracuna
curl -d '{"EBeton":4.4E+04}' -H "Content-Type: application/json" -X POST http://localhost:8080/pakrunner/rest/api/start

### Da li proracun radi?
curl -H "Content-Type: application/json" -X GET http://localhost:8080/pakrunner/rest/api/isrunning

### Zaustavljanje
curl -d '' -H "Content-Type: application/json" -X POST http://localhost:8080/pakrunner/rest/api/stop

### Poslednjih N linija loga (stdout)
curl -d '' -H "Content-Type: application/json" -X GET http://localhost:8080/pakrunner/rest/api/logtail/4

### ZIP-ovani rezultati (fajl 'rezultati.zip')
curl -d '' -H "Content-Type: application/json" -X GET http://localhost:8080/pakrunner/rest/api/getresults

# pakrunner

**Pakrunner** je REST servis za kontrolu i monitoring dugotrajnih proračuna. Servis je nastao iz potrebe da se dugotrajni proračuni zasnovani na metodi konačnih elemenata pokreću preko standardizovanog REST interfejsa iz bilo koje aplikacije, sa bilo kojeg operativnog sistema, običnim HTTP pozivima. Pored mogućnosti pokretanja proračuna, servis omogućava i mnoge druge funkcionalnosti, kao što su čitanje logova u realnom vremenu, rad sa posebnim poslovima, upit statusa, operacije sa fajlovima i direktorijumima, _upload_ ulaznih fajlova, preuzimanje rezultata proračuna itd.

Zamišljeno je da servis radi u okviru bezbednog okruženja, kao što je VPN (_Virtual Private Network_), pa u protokol za sada nije ugrađeno ništa od sigurnosnih protokola. Implementacija sigurnosnih mehanizama planirana je za narednu verziju. Servis je napisan korišćenjem programskog jezika Java, kao i dodatnih biblioteka. Neke od dodatnih biblioteka su _Jackson_ za rad sa JSON dokumentima, _Apache Commons_ za rad sa fajl sistemom, _Jersey_ za realizaciju HTTP metoda POST i GET itd. Za izgradnju se koristi _Maven_. Kao _servlet_ kontejner testiran je _Apache Tomcat_, ali se može uzeti i neki drugi. Iako je softver testiran samo na Linuxu, trebalo bi da bez problema funkcioniše i na drugim operativnim sistemima koji podržavaju Javu, obzirom da su tokom razvoja izbegavani direktni sistemski pozivi. 

Aplikacija **pakrunner** se isporučuje u standardnom WAR (_Web ARchive_) formatu, uz konfiguraciju u fajlu **config.properties**, koji postavlja sledeće varijable:
```
# Direktorijum u kome se nalaze fajlovi potrebni za proračune
MASTER_DIR = /home/milos/pakrunner/master
# Radni direktorijum u kome se kreiraju poddirektorijumi sa poslovima
RESULT_DIR = /home/milos/pakrunner/proracuni
# Komanda za terminaciju stabla procesa (argument je naziv posla)
KILL_PROCESSES_COMMAND = /home/milos/pakrunner/kill_processes.sh
# Naziv log fajla
LOG_FILE = pak.log
# Naziv ZIP-a sa rezultatima proračuna
RESULT_ZIP = results.zip
```
## Primeri poziva
Osnovna jedinica rada je **posao**, koji se kreira pozivom **/createnew**, kopiranjem sadržaja direktorijuma **MASTER_DIR** u direktorijum **RESULT_DIR/GUID**. Kopiranje direktorijuma NIJE rekurzivno. Ukoliko dati posao već postoji, nastaje greška. Kreiranje posla NE POKREĆE proračun, već ga samo priprema:

### Kreiranje novog posla
`curl -d '{"guid":"3333-4444"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/createnew`

Izlaz većine poziva je JSON, sa dva obavezna polja. Prvo polje je **status** operacije, koje može biti **true** ili **false**, u zavisnosti da li je poziv ispravno obavljen ili je došlo do greške. Drugo obavezno polje povratnog JSON-a je **message**, koje u slučaju greške sadrži njen bliži opis. U nastavku će biti dati pojedinačni pozivi, sa nekim specifičnim detaljima. 

### Echo poziv
Za svrhu testiranja odziva servisa i ispravnosti JSON-a u smislu formata, postoji poziv koji vraća isti JSON koji mu je poslat:

`curl -d '{"guid":"3333-5555", "command":"./proba.sh"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/echo`

### Pokretanje posla
Kreiran posao se ne pokreće automatski. Potrebno je uz GUID navesti i komandu koja pokreće proračun:

`curl -d '{"guid":"3333-4444", "command":"./proba.sh"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/start`

### Upit statusa
Status pokrenutog posla se može ispitati upitom. Vraća se **true** u slučaju da proračun trenutno radi, kao i vreme u sekundama koje je proveo u tekućem statusu:

`curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/isrunning/3333-4444`

### Zaustavljanje posla
Posao se može u svakom trenutku zaustaviti jednostavnim pozivom. Terminacija procesa implicira i terminaciju procesa koje je osnovni proces eventualno pokrenuo. 

`curl -d '{"guid":"3333-4444"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/stop`

### Trenutno aktivan posao
Vraća se GUID trenutno aktivnog posla i status **true** u slučaju da bilo koji proračun trenutno radi. U suprotnom se vraća status **false** i prazan string za GUID.

`curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/runningtask`

### Lista poslova
Lista tekućih poslova u direktorijumu RESULT_DIR može se dobiti sledećim GET pozivim:

`curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/tasklist`

### Poslednjih nekoliko linija loga (*logtail*)
Poslednjih `n` linija loga za posao GUID. Ako je `n`=0, preuzima se ceo log:

`curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/logtail/3333-4444/4`

### Preuzimanje log fajla
za posao GUID, preuzimanje celog loga u formi priloga (*attachment*), vrši se pomoću:

`curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/logdownload/3333-4444`

### Preuzmi rezultate
Rezultati (ili bilo koji fajlovi u argumentu `files`) iz nekog posla se mogu preuzeti u formi priloga pomoću poziva:

`curl -d '{"guid":"3333-4444", "files":["proba.sh","pak.log"]}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/getresults --output rezultati.zip`

### Uklanjanje posla
Ceo posao se može ukloniti. Pre toga se zaustavlja ukoliko je proračun bio aktivan:

`curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/remove/3333-4444/`

### Brisanje svih poslova
Svi poslovi se mogu obrisati jednim GET pozivom:

`curl -H "Content-Type: application/json" -X GET http://147.91.200.5:8081/pakrunner/rest/api/removeall`

### Upload ZIP-a u posao 
*Upload* i raspakovavanje ZIP fajla u radni direktorijum posla GUID vrši se sledećim pozivom:

`curl -F 'file=@proba.zip' -F 'guid=3333-1111' -X POST http://147.91.200.5:8081/pakrunner/rest/api/uploadzip`

### Kopiranje fajla 
Kopiranje se vrši iz osnovnog MASTER_DIR ili nekog njegovog podfoldera u radni direktorijum posla GUID. Navodi se relativna putanja fajla koji se kopira i ciljno ime fajla: 

`curl -d '{"guid":"3333-1111", "path":"L10/ttt.txt", "name":"ttt-kopija.txt"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/localcopy`

### Kopiranje fajla iz jednog posla u drugi
Kopiranje se vrši iz radnog direktorijuma `guidsrc` u direktorijum `guiddest`, pri čemu je moguće zadati i novo ime fajla. 

`curl -d '{"guidsrc":"3333-1111", "guiddest":"3333-2222", "namesrc":"pom.xml", "namedest":"pom.xml"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/copyfiletasktotask`

### Brisanje fajla iz radnog direktorijuma
U svrhu brisanja fajla iz radnog direktorijuma posla, koristi se sledeći poziv. Može se navesti i relativna putanja. 

`curl -d '{"guid":"3333-1111", "path":"ttt.txt"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/removefile`

### Preimenovanje fajla u radnom direktorijumu
U svrhu preimenovanja fajla iz radnog direktorijuma posla, koristi se sledeći poziv. Mogu se navesti i relativne putanje. 

`curl -d '{"guid":"3333-1111", "pathold":"Ulaz.csv", "pathnew":"Ulaz1.csv"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/renamefile`

### Listing direktorijuma
Ako je `path` prazan, lista se radni direktorijum GUID, a ako `path` nije prazan string, vraća se sadržaj direktorijuma `MASTER_DIR/path`! Vraća posebno niz fajlova, a posebno niz direktorijuma.

`curl -d '{"guid":"3333-4444", "path":"/L10"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/listfiles`

### Izvršavanje kratkog posla
Ovaj poziv se koristi za razne pomoćne skriptove (kopiranje, brisanje, promena prava pristupa...) za koje se ne očekuje da dugo traju. Poziv je blokirajući, a ovakvi pozivi se loguju u poseban log fajl pod nazivom **shorttask.log**.

`curl -d '{"guid":"3333-4444", "command":"./proba1.sh"}' -H "Content-Type: application/json" -X POST http://147.91.200.5:8081/pakrunner/rest/api/runshorttask`

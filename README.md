```
   __                       
  / /__________  _________ _
 / __/ ___/ __ \/ ___/ __ `/
/ /_/ /  / /_/ / /__/ /_/ / 
\__/_/   \____/\___/\__,_/  
```

Summary
=======================================
**troca** is a cross-platform extensible command-line currency rates querier, written in Java.


Usage Examples
=======================================
```
$ ./troca 
troca - cross-platform currency rates querier

Usage:
  -f | --from currency1[,currency2,currency3,...] : currency to convert from
  -t | --to currency1[,currency2,currency3,...]   : currency to convert to
  [ -v | --verbose ]                              : verbose output, incompatible
                                                    with --quiet.
  [ -s | --source data_source ]                   : choose data source
  [ -j | --json [ --fancy ] ]                     : JSON output [ formatted ],
                                                    incompatible with --quiet
  [ --db-type database_type ]                     : Type of database to add
                                                    results into. Requires --db
  [ --db database_link ]                          : Link to database. Depends
                                                    on database type.
                                                    Requires --db-type.
  [ --daemon N ]                                  : Repeat query every N ms,
                                                    N = 5000+ (respect the data
                                                    sources servers, please!
  [ --timestamp ]                                 : Prepend timestamps to output.
                                                    Incompatible with --quiet.
  [ -q | --quiet ]                                : Print nothing, requires --db.

Data sources:
  y | yahoo    : Yahoo Finance (default) -- supports most of world currencies
  c | coinbase : Coinbase.com -- accurate rates of Bitcoin to fiat currencies

Database types:
  j | json     : JSON file
                 Requires --db to be a file path.
                 Specific parameters:
                   --db-type-json-force : If appending to JSON database fails
                                          due to JSON code corruption, allow
                                          troca to delete minimal-sufficient
                                          parts of corrupted JSON code to
                                          restore its validity. It can delete the
                                          whole file though, if its condition
                                          is unrestorable poor, so be careful.

About:
  Version 0.0.2alpha.
  Written by Raegdan [ raegdan@gmail.com ].
  License: GNU GPL v3.
  "troca" is the portuguese for "exchange".

$ ./troca -f usd,eur,gbp -t rub,uah --source yahoo
GBP/RUB 83.0121
GBP/UAH 24.6389
EUR/RUB 65.2685
USD/RUB 53.3
USD/UAH 15.82
EUR/UAH 19.3724

$ ./troca -f btc -t eur,jpy,cny --source coinbase --json
{"BTC/CNY":1980.183537,"BTC/JPY":38431.223504,"BTC/EUR":261.040573}
```

Extensibility
=======================================
**troca** is extensible project.
To add your favourite data source / data storage engine to troca, you need just to:
* inherit a new class from ExchangeAPI / TrendStorage and override queryRates() / storeRates() method with an implementation of needed API;
* add an item to switch in main();
* got it!


Feedback
=======================================
I'm always open for any feedback, do not hesitate to write me.


Third party code
=======================================
**troca** uses FasterXML Jackson libraries to handle JSON. They are libre software under Apache License 2.0, so they may legally be a part of GPL v3 software. The libraries are included into this repo in built form because I don't need to modify their sources; but if you do - you may always get them from the FasterXML GitHub page.

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
$ ./troca --help
troca - cross-platform currency rates querier

Usage:
  -f | --from currency1[,currency2,currency3,...] : currency to convert from
  -t | --to currency1[,currency2,currency3,...]   : currency to convert to
  [ -v | --verbose ]                              : verbose output
  [ -s | --source data_source ]                   : choose data source
  [ -j | --json ]                                 : JSON output (not human-readable, 
                                                    but simply parseable)

Data sources:
  y | yahoo    : Yahoo Finance (default) -- supports most of world currencies
  c | coinbase : Coinbase.com -- accurate rates of Bitcoin to fiat currencies

About:
  Written by Raegdan [raegdan@gmail.com]
  License: GNU GPL v3
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
To add your favourite data source to troca, you need just to:
* inherit a new class from ExchangeAPI and override queryRates() method with an implemenation of an API you need to request data from;
* add an item to switch in main();
* got it!


Feedback
=======================================
I'm always open for any feedback, do not hesitate to write me.

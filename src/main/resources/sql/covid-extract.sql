select * 
  from tap_covid_19.exchange_rate as exch_rates inner
  join tap_covid_19.c19_trk_us_daily as us_dailies
    on TO_CHAR(exch_rates.date :: DATE, 'yyyy-mm-dd') = us_dailies.date
 limit :#resultLimit

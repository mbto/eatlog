REM Eatlog uses geo tables from https://github.com/mbto/maxmind-geoip2-csv2sql-converter
REM Example build configuration with RU,EN locales and IPv4:
chcp 65001
set JAVA_HOME=C:\Program Files\Java\jdk-11.0.11\
"C:\mm\maxmind-geoip2-csv2sql-converter-1.1\bin\maxmind-geoip2-csv2sql-converter.bat" -od "C:\mm\maxmind-geoip2-csv2sql-converter-1.1\eatlog" -oa "" -k M8jFjv3vZWrOCaZN -c "C:\Users\%USERNAME%\IdeaProjects\eatlog\GeoLite2-City-CSV.mysql.eatlog.ini" -i 4 -l "ru,en" -dc true
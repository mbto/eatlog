select
 ipv4.v_geoname_id geoname_id,
 country.emoji,
 country.name_ru country_ru,
 country.name_en country_en,
 city.name_ru city_ru,
 city.name_en city_en,
 subdivision1.name_ru subDiv1_ru,
 subdivision1.name_en subDiv1_en,
 subdivision2.name_ru subDiv2_ru,
 subdivision2.name_en subDiv2_en
from
 {0}.ipv4
join {0}.city on ipv4.v_geoname_id = city.geoname_id
join {0}.country on city.country_id = country.id
left join {0}.subdivision1 subdivision1 on city.subdivision1_id = subdivision1.id
left join {0}.subdivision2 subdivision2 on city.subdivision2_id = subdivision2.id
where {0}.city.geoname_id = find_geoname_id_by_ip({1})
limit 1
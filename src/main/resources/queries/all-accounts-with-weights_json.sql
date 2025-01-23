select
 a.*,
 concat('{',group_concat('"',DATE_FORMAT(w.date,'%d.%m.%Y'),'":',w.kilogram order by w.date separator ','), '}')
 as weights_json
from account a
left join weight w on a.id = w.account_id
where ({0} is null or a.id = {0})
group by a.id
limit {1}, {2}
select
 s.id store_id,
 p.id product_id,
 p.title,
 count(c.id) consumed_count,
 /*s.account_id,group_concat(c.id),*/
 p.portion_gram, p.b, p.j, p.u, p.kkal
from store s
join product p on s.id = p.store_id
left join consumed c on p.id = c.product_id
where s.account_id = {0}
and s.id = {1}
group by p.id
order by count(c.id) desc, s.id, p.title
with cps as (
SELECT
 s.id store_id,
 p.id product_id,
 c.id consumed_id,
 c.date,
 p.title,
 p.b,
 p.j,
 p.u,
 p.b_to_kkal,
 p.j_to_kkal,
 p.u_to_kkal,
 p.kkal,
 p.portion_gram,
 c.gram consumed_gram,
 round(c.gram * p.b / 100, 2) consumed_b,
 round(c.gram * p.j / 100, 2) consumed_j,
 round(c.gram * p.u / 100, 2) consumed_u,
 round(round(c.gram * p.b / 100, 2) * 4, 2) consumed_b_to_kkal,
 round(round(c.gram * p.j / 100, 2) * 9, 2) consumed_j_to_kkal,
 round(round(c.gram * p.u / 100, 2) * 4, 2) consumed_u_to_kkal,
 round(
  (round(c.gram * p.b / 100, 2) * 4
  + round(c.gram * p.j / 100, 2) * 9
  + round(c.gram * p.u / 100, 2) * 4), 2) consumed_kkal
FROM consumed c
join product p on c.product_id = p.id
join store s on p.store_id = s.id
where s.account_id = {0}
and ({1} is null or c.date = {1}) /* for select one */
and c.date in ({2}) /* for select all paginated */
)
select store_id,product_id,consumed_id,date,title,
b,j,u,
b_to_kkal,j_to_kkal,u_to_kkal,kkal,
portion_gram,
consumed_gram,
consumed_b,consumed_j,consumed_u,
consumed_b_to_kkal,consumed_j_to_kkal,consumed_u_to_kkal,consumed_kkal
from cps
union all
select null,null,null,date,'сумма',
sum(b),sum(j),sum(u),
sum(b_to_kkal),sum(j_to_kkal),sum(u_to_kkal),sum(kkal),
sum(portion_gram),
sum(consumed_gram),
sum(consumed_b),sum(consumed_j),sum(consumed_u),
sum(consumed_b_to_kkal),sum(consumed_j_to_kkal),sum(consumed_u_to_kkal),
round(sum(consumed_kkal), -1)
from cps
group by date
order by date, -consumed_id desc
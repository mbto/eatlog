SELECT
 w.id weight_id,
 w.date,
 w.kilogram,
 l.title,
 l.b,
 l.j,
 l.u,
 l.b_to_kkal,
 l.j_to_kkal,
 l.u_to_kkal,
 round(w.kilogram * l.b, 2) b_max,
 round(w.kilogram * l.j, 2) j_max,
 round(w.kilogram * l.u, 2) u_max,
 round(w.kilogram * l.b_to_kkal, 2) b_max_to_kkal,
 round(w.kilogram * l.j_to_kkal, 2) j_max_to_kkal,
 round(w.kilogram * l.u_to_kkal, 2) u_max_to_kkal
FROM
 weight w
 LEFT JOIN limitation l ON w.account_id = l.account_id
WHERE w.account_id = {0}
and ({1} is null or w.date = {1}) /* for select one */
and w.date in ({2}) /* for select all paginated */
ORDER BY w.date, l.id
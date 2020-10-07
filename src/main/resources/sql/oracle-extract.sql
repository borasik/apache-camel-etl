select acc.*,ent.* from gtp_account acc join gtp_entity_account ent on acc.account_id = ent.account_id
WHERE rownum <= 10
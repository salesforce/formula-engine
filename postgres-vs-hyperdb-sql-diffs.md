# Postgres vs Hyper DB SQL Expression Differences

## Translation Patterns

| Pattern | Postgres | Hyper DB |
|---------|----------|----------|
| A | `pg_catalog.make_interval()` | `INTERVAL arithmetic` |
| B | `::timestamp(0)` | `::timestamp` |
| C | `TO_CHAR(TO_TIMESTAMP(),'SSSS.MS')` | `EXTRACT(EPOCH FROM TO_TIMESTAMP())` |
| D | `::numeric(40,20)` | `::numeric(38,18)` |

## Gold File Changelog (54 files differ)

### Pattern A+B: pg_catalog.make_interval() -> INTERVAL arithmetic, ::timestamp(0) -> ::timestamp

| Test Name | Postgres SQL | Hyper DB SQL |
|-----------|-------------|-------------|
| testAddDate | `($!s0s!$.customdate1__c+pg_catalog.make_interval(0,0,0,0,0,0,TRUNC(COALESCE($!s0s!$.customnumber1__c, 0))*86400.0))::...` | `($!s0s!$.customdate1__c+(INTERVAL '1 second'*TRUNC(COALESCE($!s0s!$.customnumber1__c, 0))*86400.0))::timestamp` |
| testAddDateTime | `($!s0s!$.customdatetime1__c+pg_catalog.make_interval(0,0,0,0,0,0,COALESCE($!s0s!$.customnumber1__c, 0)*86400.0))::tim...` | `($!s0s!$.customdatetime1__c+(INTERVAL '1 second'*COALESCE($!s0s!$.customnumber1__c, 0)*86400.0))::timestamp` |
| testAddDateTimeGivingDate | `($!s0s!$.customdatetime1__c+pg_catalog.make_interval(0,0,0,0,0,0,COALESCE($!s0s!$.customnumber1__c, 0)*86400.0))::tim...` | `($!s0s!$.customdatetime1__c+(INTERVAL '1 second'*COALESCE($!s0s!$.customnumber1__c, 0)*86400.0))::timestamp` |
| testAddDateTimeGivingDateValue | `DATE_TRUNC('DAY', (($!s0s!$.customdatetime1__c+pg_catalog.make_interval(0,0,0,0,0,0,COALESCE($!s0s!$.customnumber1__c...` | `DATE_TRUNC('DAY', (($!s0s!$.customdatetime1__c+(INTERVAL '1 second'*COALESCE($!s0s!$.customnumber1__c, 0)*86400.0))::...` |
| testAddDateTimeMinutes | `($!s0s!$.customdatetime1__c+pg_catalog.make_interval(0,0,0,0,0,0,(COALESCE($!s0s!$.customnumber1__c, 0)/1440)*86400.0...` | `($!s0s!$.customdatetime1__c+(INTERVAL '1 second'*(COALESCE($!s0s!$.customnumber1__c, 0)/1440)*86400.0))::timestamp` |
| testAddDateTimeWithExpr | `(((($!s0s!$.customdatetime1__c+pg_catalog.make_interval(0,0,0,0,0,0,COALESCE(($!s0s!$.custompercent1__c / 100.0), 0)*...` | `(((($!s0s!$.customdatetime1__c+(INTERVAL '1 second'*COALESCE(($!s0s!$.custompercent1__c / 100.0), 0)*86400.0))::times...` |
| testAddDateWithExpr | `(((($!s0s!$.customdate1__c+pg_catalog.make_interval(0,0,0,0,0,0,TRUNC(COALESCE(($!s0s!$.custompercent1__c / 100.0), 0...` | `(((($!s0s!$.customdate1__c+(INTERVAL '1 second'*TRUNC(COALESCE(($!s0s!$.custompercent1__c / 100.0), 0))*86400.0))::ti...` |
| testIfTextCompareEqualReturnDate | `CASE WHEN (COALESCE($!s0s!$.customtext1__c, CONCAT($!s0s!$.customemail1__c, 'x'))=COALESCE($!s0s!$.customemail1__c, C...` | `CASE WHEN (COALESCE($!s0s!$.customtext1__c, CONCAT($!s0s!$.customemail1__c, 'x'))=COALESCE($!s0s!$.customemail1__c, C...` |
| testIfTextCompareEqualReturnDateTime | `CASE WHEN (COALESCE($!s0s!$.customphone1__c, CONCAT($!s0s!$.customphone2__c, 'x'))=COALESCE($!s0s!$.customphone2__c, ...` | `CASE WHEN (COALESCE($!s0s!$.customphone1__c, CONCAT($!s0s!$.customphone2__c, 'x'))=COALESCE($!s0s!$.customphone2__c, ...` |
| testSubDateTimeGivingDate | `($!s0s!$.customdatetime1__c-pg_catalog.make_interval(0,0,0,0,0,0,COALESCE($!s0s!$.customnumber1__c, 0)*86400.0))::tim...` | `($!s0s!$.customdatetime1__c-(INTERVAL '1 second'*COALESCE($!s0s!$.customnumber1__c, 0)*86400.0))::timestamp` |
| testSubDateTimeGivingDateTime | `($!s0s!$.customdatetime1__c-pg_catalog.make_interval(0,0,0,0,0,0,COALESCE($!s0s!$.customnumber1__c, 0)*86400.0))::tim...` | `($!s0s!$.customdatetime1__c-(INTERVAL '1 second'*COALESCE($!s0s!$.customnumber1__c, 0)*86400.0))::timestamp` |
| testSubDateTimeGivingDateValue | `DATE_TRUNC('DAY', (($!s0s!$.customdatetime1__c-pg_catalog.make_interval(0,0,0,0,0,0,COALESCE($!s0s!$.customnumber1__c...` | `DATE_TRUNC('DAY', (($!s0s!$.customdatetime1__c-(INTERVAL '1 second'*COALESCE($!s0s!$.customnumber1__c, 0)*86400.0))::...` |

### Pattern B: ::timestamp(0) -> ::timestamp

| Test Name | Postgres SQL | Hyper DB SQL |
|-----------|-------------|-------------|
| testAddMonths | `($!s0s!$.customdate1__c +  (CASE WHEN extract(day FROM (date_trunc('month', $!s0s!$.customdate1__c) + interval '1 mon...` | `($!s0s!$.customdate1__c +  (CASE WHEN extract(day FROM (date_trunc('month', $!s0s!$.customdate1__c) + interval '1 mon...` |
| testAddMonthsDate | `($!s0s!$.customdate1__c +  (CASE WHEN extract(day FROM (date_trunc('month', $!s0s!$.customdate1__c) + interval '1 mon...` | `($!s0s!$.customdate1__c +  (CASE WHEN extract(day FROM (date_trunc('month', $!s0s!$.customdate1__c) + interval '1 mon...` |
| testAddMonthsDateTime | `($!s0s!$.customdatetime1__c +  (CASE WHEN extract(day FROM (date_trunc('month', $!s0s!$.customdatetime1__c) + interva...` | `($!s0s!$.customdatetime1__c +  (CASE WHEN extract(day FROM (date_trunc('month', $!s0s!$.customdatetime1__c) + interva...` |
| testDateTimeText | `(CONCAT(TO_CHAR(($!s0s!$.customdatetime1__c)::timestamp(0), 'YYYY-MM-DD HH24:MI:SS'), 'Z' ))` | `(CONCAT(TO_CHAR(($!s0s!$.customdatetime1__c)::timestamp, 'YYYY-MM-DD HH24:MI:SS'), 'Z' ))` |

### Pattern C: TO_CHAR(TO_TIMESTAMP(),'SSSS.MS') -> EXTRACT(EPOCH FROM TO_TIMESTAMP())

| Test Name | Postgres SQL | Hyper DB SQL |
|-----------|-------------|-------------|
| testAddBigTimeValueWithValidInValid | `MOD(CAST(TO_CHAR(TO_TIMESTAMP($!s0s!$.dateString__c, 'HH24:mi:ss.MS'),'SSSS.MS') AS NUMERIC) * 1000+ROUND(MOD(CAST(93...` | `MOD(CAST(EXTRACT(EPOCH FROM TO_TIMESTAMP($!s0s!$.dateString__c, 'HH24:mi:ss.MS')) AS NUMERIC) * 1000+ROUND(MOD(CAST(9...` |
| testAddHoursWithTwoCustFields | `MOD(CAST(TO_CHAR(TO_TIMESTAMP($!s0s!$.timeString__c, 'HH24:mi:ss.MS'),'SSSS.MS') AS NUMERIC) * 1000+ROUND(MOD(CAST(CO...` | `MOD(CAST(EXTRACT(EPOCH FROM TO_TIMESTAMP($!s0s!$.timeString__c, 'HH24:mi:ss.MS')) AS NUMERIC) * 1000+ROUND(MOD(CAST(C...` |
| testAddTimeValueWithValidInValid | `MOD(CAST(TO_CHAR(TO_TIMESTAMP($!s0s!$.dateString__c, 'HH24:mi:ss.MS'),'SSSS.MS') AS NUMERIC) * 1000+ROUND(MOD(CAST(72...` | `MOD(CAST(EXTRACT(EPOCH FROM TO_TIMESTAMP($!s0s!$.dateString__c, 'HH24:mi:ss.MS')) AS NUMERIC) * 1000+ROUND(MOD(CAST(7...` |
| testFormatDurationTime | `TO_CHAR((INTERVAL '1 second' * ABS(((CAST(TO_CHAR(TO_TIMESTAMP($!s0s!$.timeString1__c, 'HH24:mi:ss.MS'),'SSSS.MS') AS...` | `TO_CHAR((INTERVAL '1 second' * ABS(((CAST(EXTRACT(EPOCH FROM TO_TIMESTAMP($!s0s!$.timeString1__c, 'HH24:mi:ss.MS')) A...` |
| testIfErrorTextTimeValueWithValidInValid | `CASE WHEN  NOT $!s0s!$.dateString__c ~ '^([01]\d\|2[0-3]):[0-5][0-9]:[0-5][0-9]\.[0-9][0-9][0-9]$'  THEN NULL ELSE TO...` | `CASE WHEN  NOT $!s0s!$.dateString__c ~ '^([01]\d\|2[0-3]):[0-5][0-9]:[0-5][0-9]\.[0-9][0-9][0-9]$'  THEN NULL ELSE TO...` |
| testSubtractBigTimeValue | `MOD(CAST(TO_CHAR(TO_TIMESTAMP($!s0s!$.dateString__c, 'HH24:mi:ss.MS'),'SSSS.MS') AS NUMERIC) * 1000-ROUND(MOD(CAST(18...` | `MOD(CAST(EXTRACT(EPOCH FROM TO_TIMESTAMP($!s0s!$.dateString__c, 'HH24:mi:ss.MS')) AS NUMERIC) * 1000-ROUND(MOD(CAST(1...` |
| testSubtractTimeValueWithValidInValid | `MOD(CAST(TO_CHAR(TO_TIMESTAMP($!s0s!$.dateString__c, 'HH24:mi:ss.MS'),'SSSS.MS') AS NUMERIC) * 1000-ROUND(MOD(CAST(72...` | `MOD(CAST(EXTRACT(EPOCH FROM TO_TIMESTAMP($!s0s!$.dateString__c, 'HH24:mi:ss.MS')) AS NUMERIC) * 1000-ROUND(MOD(CAST(7...` |
| testSubtractTwoTimeFields | `MOD(CAST(TO_CHAR(TO_TIMESTAMP($!s0s!$.timeString2__c, 'HH24:mi:ss.MS'),'SSSS.MS') AS NUMERIC) * 1000-CAST(TO_CHAR(TO_...` | `MOD(CAST(EXTRACT(EPOCH FROM TO_TIMESTAMP($!s0s!$.timeString2__c, 'HH24:mi:ss.MS')) AS NUMERIC) * 1000-CAST(EXTRACT(EP...` |
| testTextTimeValueWithValidInValid | `TO_CHAR(TO_TIMESTAMP(TO_CHAR(CAST(TO_CHAR(TO_TIMESTAMP($!s0s!$.dateString__c, 'HH24:mi:ss.MS'),'SSSS.MS') AS NUMERIC)...` | `TO_CHAR(TO_TIMESTAMP(TO_CHAR(CAST(EXTRACT(EPOCH FROM TO_TIMESTAMP($!s0s!$.dateString__c, 'HH24:mi:ss.MS')) AS NUMERIC...` |
| testTimeValueWithValidString | `CAST(TO_CHAR(TO_TIMESTAMP('10:40:55.666', 'HH24:mi:ss.MS'),'SSSS.MS') AS NUMERIC) * 1000` | `CAST(EXTRACT(EPOCH FROM TO_TIMESTAMP('10:40:55.666', 'HH24:mi:ss.MS')) AS NUMERIC) * 1000` |
| testUnixTimestampWithTime | `TRUNC(CAST(TO_CHAR(TO_TIMESTAMP($!s0s!$.dateString__c, 'HH24:mi:ss.MS'),'SSSS.MS') AS NUMERIC) * 1000/1000)` | `TRUNC(CAST(EXTRACT(EPOCH FROM TO_TIMESTAMP($!s0s!$.dateString__c, 'HH24:mi:ss.MS')) AS NUMERIC) * 1000/1000)` |

### Pattern D: ::numeric(40,20) -> ::numeric(38,18)

| Test Name | Postgres SQL | Hyper DB SQL |
|-----------|-------------|-------------|
| testAbsUsesExp | `ABS(EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(40,20)))` | `ABS(EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(38,18)))` |
| testArcCosine | `ACOS(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(40,20)` | `ACOS(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(38,18)` |
| testArcSine | `ASIN(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(40,20)` | `ASIN(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(38,18)` |
| testArcTan2 | `ATAN2(COALESCE($!s0s!$.customnumber1__c, 0),COALESCE($!s0s!$.customnumber2__c, 0))::numeric(40,20)` | `ATAN2(COALESCE($!s0s!$.customnumber1__c, 0),COALESCE($!s0s!$.customnumber2__c, 0))::numeric(38,18)` |
| testArcTangent | `ATAN(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(40,20)` | `ATAN(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(38,18)` |
| testCosine | `COS(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(40,20)` | `COS(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(38,18)` |
| testExpSimple | `EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(40,20))` | `EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(38,18))` |
| testExpUsesAbs | `EXP(ABS(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(40,20))` | `EXP(ABS(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(38,18))` |
| testExpUsesCeil | `EXP(CASE WHEN COALESCE($!s0s!$.customnumber1__c, 0)&gt;=0 THEN CEIL(ROUND(COALESCE($!s0s!$.customnumber1__c, 0),33)) ...` | `EXP(CASE WHEN COALESCE($!s0s!$.customnumber1__c, 0)&gt;=0 THEN CEIL(ROUND(COALESCE($!s0s!$.customnumber1__c, 0),33)) ...` |
| testExpUsesFloor | `EXP(CASE WHEN COALESCE($!s0s!$.customnumber1__c, 0)&gt;=0 THEN FLOOR(ROUND(COALESCE($!s0s!$.customnumber1__c, 0),33))...` | `EXP(CASE WHEN COALESCE($!s0s!$.customnumber1__c, 0)&gt;=0 THEN FLOOR(ROUND(COALESCE($!s0s!$.customnumber1__c, 0),33))...` |
| testExpUsesIf | `EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(40,20))` | `EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(38,18))` |
| testExpUsesLen | `EXP(COALESCE(LENGTH($!s0s!$.customtext1__c),0)::numeric::numeric(40,20))` | `EXP(COALESCE(LENGTH($!s0s!$.customtext1__c),0)::numeric::numeric(38,18))` |
| testExpUsesLn | `ROUND(EXP(LN(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(40,20)), 0::integer)` | `ROUND(EXP(LN(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(38,18)), 0::integer)` |
| testExpUsesLog | `EXP(LOG(10, COALESCE($!s0s!$.customnumber1__c, 0))::numeric(40,20))` | `EXP(LOG(10, COALESCE($!s0s!$.customnumber1__c, 0))::numeric(38,18))` |
| testExpUsesMOD | `EXP(MOD(COALESCE($!s0s!$.customnumber1__c, 0), COALESCE($!s0s!$.customnumber2__c, 0))::numeric(40,20))` | `EXP(MOD(COALESCE($!s0s!$.customnumber1__c, 0), COALESCE($!s0s!$.customnumber2__c, 0))::numeric(38,18))` |
| testExpUsesMinus | `EXP((COALESCE(($!s0s!$.custompercent1__c / 100.0), 0)-COALESCE($!s0s!$.customcurrency1__c, 0))::numeric(40,20))` | `EXP((COALESCE(($!s0s!$.custompercent1__c / 100.0), 0)-COALESCE($!s0s!$.customcurrency1__c, 0))::numeric(38,18))` |
| testExpUsesPlus | `EXP(COALESCE((COALESCE(($!s0s!$.custompercent1__c / 100.0), 0)+COALESCE($!s0s!$.customnumber1__c, 0)), 0)::numeric(40...` | `EXP(COALESCE((COALESCE(($!s0s!$.custompercent1__c / 100.0), 0)+COALESCE($!s0s!$.customnumber1__c, 0)), 0)::numeric(38...` |
| testExpUsesRound | `EXP(ROUND(COALESCE($!s0s!$.customnumber1__c, 0), COALESCE($!s0s!$.customnumber2__c, 0)::integer)::numeric(40,20))` | `EXP(ROUND(COALESCE($!s0s!$.customnumber1__c, 0), COALESCE($!s0s!$.customnumber2__c, 0)::integer)::numeric(38,18))` |
| testExpUsesSqrt | `EXP(SQRT(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(40,20))` | `EXP(SQRT(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(38,18))` |
| testExpUsesValue | `EXP(CAST($!s0s!$.customtext1__c AS NUMERIC)::numeric(40,20))` | `EXP(CAST($!s0s!$.customtext1__c AS NUMERIC)::numeric(38,18))` |
| testModUsesExpCeil | `MOD(EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(40,20)), CASE WHEN COALESCE($!s0s!$.customnumber2__c, 0)&gt;=0...` | `MOD(EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(38,18)), CASE WHEN COALESCE($!s0s!$.customnumber2__c, 0)&gt;=0...` |
| testPi | `ROUND(PI()::numeric(40,20), 12::integer)` | `ROUND(PI()::numeric(38,18), 12::integer)` |
| testRoundUsesExp | `ROUND(EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(40,20)), CASE WHEN COALESCE($!s0s!$.customnumber2__c, 0)&gt;...` | `ROUND(EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(38,18)), CASE WHEN COALESCE($!s0s!$.customnumber2__c, 0)&gt;...` |
| testSine | `SIN(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(40,20)` | `SIN(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(38,18)` |
| testSqrtUsesExp | `SQRT(EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(40,20)))` | `SQRT(EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(38,18)))` |
| testTangent | `TAN(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(40,20)` | `TAN(COALESCE($!s0s!$.customnumber1__c, 0))::numeric(38,18)` |
| testTruncUsesExp | `TRUNC(EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(40,20)), CASE WHEN COALESCE($!s0s!$.customnumber2__c, 0)&gt;...` | `TRUNC(EXP(COALESCE($!s0s!$.customnumber1__c, 0)::numeric(38,18)), CASE WHEN COALESCE($!s0s!$.customnumber2__c, 0)&gt;...` |


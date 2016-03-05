Desired: 
* easy to embed for at least standalone/small/demo installs; easy to scale out for multi-tenant setup
(ideally we would just create a swarm of MOLGENIS instances and let them figure it out)
* fast batch uploads (GoNL upload in 5 mins, small dataset in seconds, 100 col * 10k records/sec)
* > 1500 columns (showstopper for mysql and postgresql? solution: page over multiple tables)
* > 64 xrefs possible (showstopper: mysql; solution: many link tables; of course other stores don't even have this so then we need to check fkeys ourselves!)
* hot backups
* scale-out (over multiple servers; solution: use a pool of servers with partition (not sharding, that is too difficult), or use distributed db like cassandra)
* easy search/elastic-search integration
* transactions (I think having no ACID will result in world-of-pain; potentially we could remove transactions on long running uploads using a 'staging' mechanism which we would like anyway to allow curation and assisted uploads).
* persistent to disk, durable
* fast range queries/pages/limit/offset
* reproduce insert ordering (what you upload is what you get, unless updating)
* rare small updates (so don't need optimized for that)
* fast access by key (assuming elasticsearch)
* optional: in place validation of xrefs, uniques, not-null, readonly
* do we need joins? if we scale-out while keeping dataset per server we want it, right? but if we have different back-ends you want it in middleware anyway!
* runtime change of existing data structures
* easy administration (e.g. whole hadoop stack is too much to require)
* high single node performance (for simple setups)
* open source
* cross platform (linux, mac, windows)

Generally, MySQL and PostgreSQL will provide most value with H2/derby as standalone version (we could partition one per project/group); other stores will only work in limited/specific use cases. The big companies still use *sql for most cases, incl Facebook and Google, and partition based on traffic.
Can we use PostgreSQL without need for ElasticSearch? http://rachbelaid.com/postgres-full-text-search-is-good-enough/

Instead, it seems we should invest in big SSD server which is a game changer. Hardware is cheap, man-power expensive.

(tokuDB = more compressed innoDB)

solr

# Features

| feature | postgresql | mysql | H2 | derby | hsql |
|---------|------------|-------|----|-------|------|
| max row size | 8kB (not varhcar/text) | 1.6TB (!) | ? | unlimited | unlimited |
| transactions | innoDB | y | y | y | ? |
| max cols | 250-1600 | 1000 | unlimited |1012 | unlimited |
| max indexes | 64 | unlimited | unlimited | 32767 | unlimited |
| row locking | y | y | y | y |
| multi-thread | per conn | smp | ? | yes | no |
| 'hot' backup | y | y | y | y | y |
| embedding | difficult | difficult (mxj) | y | y | y |
| versioning | diy | diy | diy | diy | diy |
| ddl transactions |  | no | yes? | | |

# Results on a macbook pro 2015/13" (2.7Ghz, SSD)

## Insert hundredvarchar
| test | no. inserts | time (ms) | inserts per second |
|------|-------------|-----------|--------------------|
| PostgresqlCopyHundredVarchar | 100000 | 4693ms | 21308.331557639038 inserts per second |
| H2HundredVarcharColumn | 100000 | 5841ms | 17120.35610340695 inserts per second |
| MysqlHundredVarcharColumn | 100000 | 10629ms | 9408.222786715589 inserts per second |
| MongoHundredVarchar | 100000 | 8600ms | 11627.906976744185 inserts per second |
| PostgresqlHundredVarcharColumn | 100000 | 22763ms | 4393.094056143742 inserts per second |

## Insert hundredint
| test | no. inserts | time (ms) | inserts per second |
|------|-------------|-----------|--------------------|
| H2HundredIntColumn | 100000 | 3147ms | 31776.294884016523 inserts per second |
| PostgresqlCopyHundredInt | 100000 | 3637 | 27495.188342040143 inserts per second |
| MongoHundredInt | 100000 | 6917ms | 14457.134595923088 inserts per second |
| MysqlHundredIntColumn | 100000 | 8836ms | 11317.338162064283 inserts per second |
| PostgresqlHundredIntColumn | 100000 | 13122ms | 7620.789513793629 inserts per second |
| CassandraHundredVarchar | 100000 | 35502ms | 2816.742718720072 inserts per second |

## Insert one int
| test | no. inserts | time (ms) | inserts per second |
|------|-------------|-----------|--------------------|
| MysqlOneIntColumn | 100000 | 392ms | 255102.04081632654 inserts per second |
| H2oneIntColumn | 100000 | 582ms | 171821.3058419244 inserts per second |
| MongoOneInt | 100000 | 1236ms | 80906.14886731391 inserts per second |
| PostgresqlOneIntColumn | 100000 | 2381ms | 41999.160016799666 inserts per second |

## count + retrieve of table with one int column 'where x > 50000' out of 100000
(timing of second run was taken)

| test | result |
|------|--------|
| mysql | 108ms |
| postgresl | 138ms |
| H2 | 196ms |
| mongo | 226ms |


MySQL uses innoDB, marginally optimized using larger cache sizes
N.B. when using MySql WITHOUT rewriteBatchedStatements speed is 4698.5857256965655, 5229.851995188536 and 12799.180852425445; that is factor 2.

Cassandra will only shine if multi-threaded so this is not a good bench.
PostgreSQL has a 'copy' batch loader that is fast, but should go to temp table and load really from there. Also can be used for fast downloads

# some notes

http://stackoverflow.com/questions/22128038/why-apache-cassandra-writes-are-so-slow-compared-to-mongodb-redis-mysql
http://kkovacs.eu/cassandra-vs-mongodb-vs-couchdb-vs-redis
http://www.velocitydb.com/QuickStart.aspx -> .NET only using C# objects
try couchDB?
couchbase?
riak?
couchbase? http://developer.couchbase.com/documentation/server/4.1/sdks/java-2.2/java-intro.html -> has an elasticseaerch plugin 
voltdb?
kyoto tycoon?
berkelydb?
terrastore?
jackrabbit?
derby http://db.apache.org/derby/
mariadb (to what extent differ from mysql?)

generally, we should not use 'in' and instead use inner join 

	PreparedStatement prep = conn.prepareStatement(
	    "SELECT * FROM TABLE(X INT=?) T INNER JOIN TEST ON T.X=TEST.ID");
	prep.setObject(1, new Object[] { "1", "2" });
	ResultSet rs = prep.executeQuery();

# some conclusions

H2 seems very promissing: 
* easily runs embedded
* hardly any limitations
* 'hot' backup and restore (should be admin process inside molgenis)
* seems fast(est)
* question about scalability
* question about query performance (mainly retrieve by id)
* question if fullsearch using Lucene is as good as ElasticSearch http://www.h2database.com/html/tutorial.html#fulltext 

Graph databases seem not a good fit, unless sparser matrices such as pathway data.
HBASE seems to complex to setup
voltdb -> only enterprise edition has durability (i.e. writing to disk)
couchbase -> no ACID, slow
memcached -> discontinued
riak -> interesting if we want to scale >5 machines


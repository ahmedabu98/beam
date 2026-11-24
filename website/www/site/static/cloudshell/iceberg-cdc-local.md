# Apache Iceberg CDC quickstart: local

<walkthrough-tutorial-duration duration="15"></walkthrough-tutorial-duration>

<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->
<!-- The Cloud Shell tutorial for the Iceberg CDC quickstart, run locally with a Hadoop catalog. Open it in Cloud Shell with `teachme iceberg-cdc-local.md`. -->

In this tutorial, you'll keep one Iceberg table in sync with another using change data capture
(CDC), with a streaming pipeline that applies each change as it lands.

Everything runs here in Cloud Shell, on a local Iceberg catalog, so you don't need a Google Cloud
project. You'll use three Beam SQL shells, each in its own terminal tab:

1. A live view of the changes to an `orders` table.
2. A streaming pipeline that applies those changes to an `orders_mirror` table.
3. A shell where you change `orders` and query both tables.

This tutorial follows the
[Apache Iceberg CDC quickstart](https://beam.apache.org/documentation/io/built-in/iceberg-cdc/).

## Start the Beam SQL shell

Download the script that starts the Beam SQL shell:

```sh
curl -O https://raw.githubusercontent.com/apache/beam/master/scripts/beam-sql.sh
chmod +x beam-sql.sh
```

The tables live in a local directory. If you've done this tutorial before, delete it, so you start
with empty tables:

```sh
rm -rf /tmp/beam-iceberg-cdc-quickstart
```

Start the shell with Iceberg support. The first start builds the shell, which takes a few minutes.
Later starts only take a few seconds.

```sh
./beam-sql.sh --version 2.79.0 --io iceberg
```

## Create a table and watch its changes

When the `BeamSQL>` prompt shows up, create a catalog and a database:

```sql
CREATE CATALOG quickstart TYPE 'iceberg'
PROPERTIES (
  'type' = 'hadoop',
  'warehouse' = 'file:///tmp/beam-iceberg-cdc-quickstart'
);

CREATE DATABASE IF NOT EXISTS quickstart.shop;
USE DATABASE quickstart.shop;
```

Create the source table, `orders`. Its primary key tells the pipeline later on which row each
change belongs to.

```sql
CREATE EXTERNAL TABLE orders (
  id BIGINT PRIMARY KEY NOT ENFORCED,
  item VARCHAR,
  qty INTEGER
)
TYPE 'iceberg';
```

Open a live view of the table's changes. It's empty for now. Leave it open.

```sql
SELECT * FROM CHANGES(orders)
WITH ('streaming' = true, 'starting_strategy' = 'earliest', 'poll_interval_seconds' = 1);
```

## Keep a second table in sync

Open a second terminal tab with the **+** button next to the current tab, and start another shell
in it:

```sh
./beam-sql.sh --version 2.79.0 --io iceberg
```

Connect to the catalog, and create a second table, `orders_mirror`:

```sql
CREATE CATALOG quickstart TYPE 'iceberg'
PROPERTIES (
  'type' = 'hadoop',
  'warehouse' = 'file:///tmp/beam-iceberg-cdc-quickstart'
);
USE DATABASE quickstart.shop;

CREATE EXTERNAL TABLE orders_mirror (
  id BIGINT PRIMARY KEY NOT ENFORCED,
  item VARCHAR,
  qty INTEGER
)
TYPE 'iceberg';
```

Now stream every change to `orders` into `orders_mirror`. The read side selects two metadata
columns: the change type (`_change_type`) and the sequence number of the snapshot that made the
change (`_commit_snapshot_sequence_number`). The write side uses them to apply each change by key,
and drops them before it writes. A streaming read starts at the latest snapshot by default, so
`starting_strategy` also picks up changes made before the pipeline started.

```sql
INSERT INTO orders_mirror
WITH ('mode' = 'merge-on-read',
      'change_type_column' = '_change_type',
      'triggering_frequency_seconds' = 1)
SELECT *, _change_type, _commit_snapshot_sequence_number
FROM CHANGES(orders)
WITH ('streaming' = true,
      'starting_strategy' = 'earliest',
      'poll_interval_seconds' = 1);
```

The pipeline keeps running. Leave it open.

## Write some changes

Open a third tab, and start a third shell:

```sh
./beam-sql.sh --version 2.79.0 --io iceberg
```

Connect to the catalog, and check that both tables are there:

```sql
CREATE CATALOG quickstart TYPE 'iceberg'
PROPERTIES (
  'type' = 'hadoop',
  'warehouse' = 'file:///tmp/beam-iceberg-cdc-quickstart'
);
USE DATABASE quickstart.shop;
SHOW TABLES;
```

Add three orders. Each change carries its type (`op`), and a sequence number (`seq`) that goes up
with each change to the same key:

```sql
INSERT INTO orders
WITH (
  'mode' = 'merge-on-read',
  'change_type_column' = 'op',
  'sequence_number_column' = 'seq')
  (id, item, qty, op, seq)
VALUES (1, 'apple', 3, 'INSERT', 1),
       (2, 'pear', 5, 'INSERT', 1),
       (3, 'plum', 2, 'INSERT', 1);
```

Within a few seconds, three inserts show up in the live view in the first tab. Back in this tab,
query both tables. Each one holds the same three orders.

```sql
SELECT * FROM orders;
SELECT * FROM orders_mirror;
```

## Update, delete and insert

Now change order 1, delete order 2 and add order 4, all in one snapshot. Both halves of an update,
`UPDATE_BEFORE` and `UPDATE_AFTER`, share a sequence number.

```sql
INSERT INTO orders
WITH (
  'mode' = 'merge-on-read',
  'change_type_column' = 'op',
  'sequence_number_column' = 'seq')
  (id, item, qty, op, seq)
VALUES (1, 'apple', 3, 'UPDATE_BEFORE', 2),
       (1, 'apple', 4, 'UPDATE_AFTER', 2),
       (2, 'pear', 5, 'DELETE', 2),
       (4, 'kiwi', 1, 'INSERT', 2);
```

Four more changes stream into the live view. Query both tables again:

```sql
SELECT * FROM orders;
SELECT * FROM orders_mirror;
```

`orders_mirror` now matches `orders`: 4 apples, 2 plums and 1 kiwi.

## Clean up

Press `q` in the first tab to close the live view, and press Ctrl+C in the second tab to stop the
streaming `INSERT`. Type `!quit` to leave each shell.

To delete the tables, delete their directory:

```sh
rm -rf /tmp/beam-iceberg-cdc-quickstart
```

The shell keeps its build in `~/.beam`, so it starts quickly next time. To free up that space,
delete it too:

```sh
rm -rf ~/.beam
```

## You're done

<walkthrough-conclusion-trophy></walkthrough-conclusion-trophy>

You kept one Iceberg table in sync with another, with each change streaming in as it landed.

To run the mirror on Dataflow against a Lakehouse catalog, try the Dataflow version of this
tutorial:

```sh
curl -fsSLO https://beam.apache.org/cloudshell/iceberg-cdc-dataflow.md && teachme iceberg-cdc-dataflow.md
```

To learn how the source and the sink work, read
[Stream changes between Iceberg tables with Apache Beam](https://beam.apache.org/blog/iceberg-cdc-sql/).

# Apache Iceberg CDC quickstart: Dataflow

<walkthrough-tutorial-duration duration="25"></walkthrough-tutorial-duration>

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
<!-- The Cloud Shell tutorial for the Iceberg CDC quickstart, run on Dataflow with a Lakehouse catalog. Open it in Cloud Shell with `teachme iceberg-cdc-dataflow.md`. -->

In this tutorial, you'll keep one Iceberg table in sync with another using change data capture
(CDC). The tables live in a Lakehouse catalog in your Google Cloud project, and a streaming Dataflow
job applies each change as it lands.

You'll use three Beam SQL shells:

1. A live view of the changes to an `orders` table.
2. A shell that starts the Dataflow job, which applies those changes to an `orders_mirror` table.
3. A shell where you change `orders` and query both tables.

The first and third sit side by side in one terminal tab, so you can watch the changes arrive as
you make them. The second runs in a tab of its own.

You need a Google Cloud project with billing enabled. The Dataflow job and the bucket cost money
while they exist, so the last step deletes them.

This tutorial follows the
[Apache Iceberg CDC quickstart](https://beam.apache.org/documentation/io/built-in/iceberg-cdc/).

## Choose a project and a bucket

Set the project to use, replacing `YOUR_PROJECT` with your project ID. The tables go in a new Cloud
Storage bucket, named after the project here. Change `BUCKET` if you'd like another name.

```sh
gcloud config set project YOUR_PROJECT
PROJECT=$(gcloud config get-value project)
BUCKET=$PROJECT-iceberg-cdc
REGION=us-central1
```

These variables only exist in this terminal tab, so do the whole setup here.

## Create the bucket and the catalog

Turn on the APIs, create the bucket, and create a Lakehouse catalog on it:

```sh
gcloud services enable biglake.googleapis.com dataflow.googleapis.com
gcloud storage buckets create gs://$BUCKET --location=$REGION --uniform-bucket-level-access
gcloud biglake iceberg catalogs create $BUCKET \
  --catalog-type=gcs-bucket --credential-mode=vended-credentials
```

The catalog hands out short-lived credentials for the bucket, so pipelines don't need access to the
bucket of their own. It makes those credentials with a service account of its own, which needs to
use the bucket:

```sh
CATALOG_ACCOUNT=$(gcloud biglake iceberg catalogs describe $BUCKET --format=json \
  | python3 -c 'import json, sys; print(json.load(sys.stdin)["biglake-service-account"])')
gcloud storage buckets add-iam-policy-binding gs://$BUCKET \
  --member=serviceAccount:$CATALOG_ACCOUNT --role=roles/storage.objectUser
```

## Let Dataflow use the catalog

Dataflow's workers run as your project's Compute Engine default service account. Let it run
Dataflow work, use the catalog, and keep temporary files in the bucket:

```sh
WORKER_ACCOUNT=$(gcloud projects describe $PROJECT --format='value(projectNumber)')-compute@developer.gserviceaccount.com
for ROLE in roles/dataflow.worker roles/biglake.editor roles/serviceusage.serviceUsageConsumer; do
  gcloud projects add-iam-policy-binding $PROJECT --member=serviceAccount:$WORKER_ACCOUNT \
    --role=$ROLE --condition=None --quiet > /dev/null
done
gcloud storage buckets add-iam-policy-binding gs://$BUCKET \
  --member=serviceAccount:$WORKER_ACCOUNT --role=roles/storage.objectAdmin
```

If you can't grant roles in this project, ask someone who can.

## Save the catalog for the shells

Every shell connects to the catalog with the same statement. Save it to a file, with your project
and bucket filled in. Save the options that send a pipeline to Dataflow too:

```sh
mkdir -p ~/iceberg-cdc
cat > ~/iceberg-cdc/catalog.sql <<EOF
CREATE CATALOG quickstart TYPE 'iceberg'
PROPERTIES (
  'type' = 'rest',
  'uri' = 'https://biglake.googleapis.com/iceberg/v1/restcatalog',
  'warehouse' = 'gs://$BUCKET',
  'header.x-goog-user-project' = '$PROJECT',
  'rest.auth.type' = 'google',
  'io-impl' = 'org.apache.iceberg.gcp.gcs.GCSFileIO',
  'header.X-Iceberg-Access-Delegation' = 'vended-credentials'
);
EOF
cat > ~/iceberg-cdc/dataflow.sql <<EOF
SET runner = DataflowRunner;
SET project = '$PROJECT';
SET region = '$REGION';
SET tempLocation = 'gs://$BUCKET/tmp';
EOF
cat ~/iceberg-cdc/catalog.sql ~/iceberg-cdc/dataflow.sql
```

## Start the Beam SQL shell

Download the script that starts the Beam SQL shell, and start it with Iceberg support. Start it from
your home directory, where the shell finds the files you just saved. The first start builds the
shell, which takes a few minutes. Later starts only take a few seconds.

```sh
cd ~
curl -O https://raw.githubusercontent.com/apache/beam/master/scripts/beam-sql.sh
chmod +x beam-sql.sh
./beam-sql.sh --version 2.79.0 --io iceberg
```

## Create a table and watch its changes

When the `BeamSQL>` prompt shows up, connect to the catalog and create a database:

```sql
!run iceberg-cdc/catalog.sql
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

## Keep a second table in sync on Dataflow

Open a second terminal tab with the **+** button next to the current tab. Start a shell there with
the Dataflow runner. Its first start builds another shell, so it takes a few minutes too.

```sh
./beam-sql.sh --version 2.79.0 --runner dataflow --io iceberg
```

Connect to the catalog, create a second table, `orders_mirror`, and send this shell's pipelines to
Dataflow:

```sql
!run iceberg-cdc/catalog.sql
USE DATABASE quickstart.shop;

CREATE EXTERNAL TABLE orders_mirror (
  id BIGINT PRIMARY KEY NOT ENFORCED,
  item VARCHAR,
  qty INTEGER
)
TYPE 'iceberg';

!run iceberg-cdc/dataflow.sql
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

This starts a streaming job on Dataflow. Its workers take a few minutes to start, and the job keeps
running until you cancel it. You can follow it on the
[Dataflow jobs page](https://console.cloud.google.com/dataflow/jobs).

## Write some changes

Go back to the first tab, with the live view, and split it in two: press Ctrl+B, then `%`. The
live view stays on the left, so you can watch it while you work in the new pane on the right. To
move between the panes later, press Ctrl+B and then an arrow key.

Start a third shell in the right pane:

```sh
./beam-sql.sh --version 2.79.0 --io iceberg
```

Connect to the catalog, and check that both tables are there:

```sql
!run iceberg-cdc/catalog.sql
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

Within a few seconds, three inserts show up in the live view on the left. Query both tables. `orders_mirror` gets the same three orders once the Dataflow job is running,
which can take a few minutes the first time.

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

Press `q` in the left pane to close the live view, and press Ctrl+C in the second tab. Type
`!quit` to leave each shell.

The Dataflow job runs until it's cancelled. Check whether it's still running, and if it is, cancel
it with its ID:

```sh
gcloud dataflow jobs list --status=active --region=us-central1
gcloud dataflow jobs cancel JOB_ID --region=us-central1
```

Then delete the catalog, and the bucket with the tables in it. Run this in the left pane of the
first tab, where you set `BUCKET`:

```sh
gcloud biglake iceberg catalogs delete $BUCKET --quiet
gcloud storage rm --recursive gs://$BUCKET
rm -rf ~/iceberg-cdc
```

The roles you granted Dataflow's service account stay in place. Remove them on the
[IAM page](https://console.cloud.google.com/iam-admin/iam) if you don't need them.

## You're done

<walkthrough-conclusion-trophy></walkthrough-conclusion-trophy>

You kept one Iceberg table in sync with another, with a streaming Dataflow job applying each change
as it landed.

To try the same thing without a Google Cloud project, try the local version of this tutorial:

```sh
curl -fsSLO https://beam.apache.org/cloudshell/iceberg-cdc-local.md && teachme iceberg-cdc-local.md
```

To learn how the source and the sink work, read
[Stream changes between Iceberg tables with Apache Beam](https://beam.apache.org/blog/iceberg-cdc-sql/).

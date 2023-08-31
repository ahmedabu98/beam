/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This tfvars file represents a Vertex AI Featurestore based on data generated
 * from https://github.com/synthetichealth/synthea and stored in Google Cloud
 * FHIR Store with BigQuery streaming.
 * See: https://cloud.google.com/healthcare-api/docs/how-tos/fhir-bigquery-streaming
 * for more details.
 */

region = "us-central1"

featurestore = {
  name_prefix      = "synthea"
  fixed_node_count = 1
  entity_types     = {
    // Flags whether patient has condition identified by the Snomed code.
    // Featurestore indexes features by an ID and a timestamp to reflect
    // what is known about a feature at a given time. A tuple of a patient's
    // id, timestamp and condition flags represents known active conditions
    // of a patient at that point in time.
    // See https://browser.ihtsdotools.org/ for more information about
    // Snomed codes.
    conditions = {
      description = "Flags whether patient has condition identified by the Snomed code."
      features    = {
        snomed_10509002          = "BOOL"
        snomed_105531004         = "BOOL"
        snomed_10939881000119105 = "BOOL"
        snomed_109838007         = "BOOL"
        snomed_110030002         = "BOOL"
        snomed_110359009         = "BOOL"
        snomed_111282000         = "BOOL"
        snomed_111287006         = "BOOL"
        snomed_1121000119107     = "BOOL"
        snomed_1231000119100     = "BOOL"
        snomed_124171000119105   = "BOOL"
        snomed_125601008         = "BOOL"
        snomed_125605004         = "BOOL"
        snomed_126906006         = "BOOL"
        snomed_127013003         = "BOOL"
        snomed_127295002         = "BOOL"
        snomed_128188000         = "BOOL"
        snomed_128613002         = "BOOL"
        snomed_132281000119108   = "BOOL"
        snomed_14760008          = "BOOL"
        snomed_1501000119109     = "BOOL"
        snomed_1551000119108     = "BOOL"
        snomed_156073000         = "BOOL"
        snomed_157141000119108   = "BOOL"
        snomed_15724005          = "BOOL"
        snomed_157265008         = "BOOL"
        snomed_15777000          = "BOOL"
        snomed_15802004          = "BOOL"
        snomed_160701002         = "BOOL"
        snomed_160903007         = "BOOL"
        snomed_160904001         = "BOOL"
        snomed_160968000         = "BOOL"
        snomed_16114001          = "BOOL"
        snomed_161665007         = "BOOL"
        snomed_161679004         = "BOOL"
        snomed_162573006         = "BOOL"
        snomed_162864005         = "BOOL"
        snomed_171131006         = "BOOL"
        snomed_1734006           = "BOOL"
        snomed_183996000         = "BOOL"
        snomed_185086009         = "BOOL"
        snomed_1871000124103     = "BOOL"
        snomed_19169002          = "BOOL"
        snomed_192127007         = "BOOL"
        snomed_195662009         = "BOOL"
        snomed_195967001         = "BOOL"
        snomed_196416002         = "BOOL"
        snomed_197927001         = "BOOL"
        snomed_198992004         = "BOOL"
        snomed_201834006         = "BOOL"
        snomed_203082005         = "BOOL"
        snomed_213150003         = "BOOL"
        snomed_221360009         = "BOOL"
        snomed_22298006          = "BOOL"
        snomed_224295006         = "BOOL"
        snomed_224299000         = "BOOL"
        snomed_224355006         = "BOOL"
        snomed_230265002         = "BOOL"
        snomed_230690007         = "BOOL"
        snomed_232353008         = "BOOL"
        snomed_233604007         = "BOOL"
        snomed_233678006         = "BOOL"
        snomed_234466008         = "BOOL"
        snomed_235595009         = "BOOL"
        snomed_235919008         = "BOOL"
        snomed_236077008         = "BOOL"
        snomed_237602007         = "BOOL"
        snomed_239720000         = "BOOL"
        snomed_239872002         = "BOOL"
        snomed_239873007         = "BOOL"
        snomed_24079001          = "BOOL"
        snomed_241929008         = "BOOL"
        snomed_246677007         = "BOOL"
        snomed_248595008         = "BOOL"
        snomed_249497008         = "BOOL"
        snomed_25064002          = "BOOL"
        snomed_254632001         = "BOOL"
        snomed_254637007         = "BOOL"
        snomed_254837009         = "BOOL"
        snomed_25675004          = "BOOL"
        snomed_262521009         = "BOOL"
        snomed_262574004         = "BOOL"
        snomed_263102004         = "BOOL"
        snomed_266934004         = "BOOL"
        snomed_266948004         = "BOOL"
        snomed_267020005         = "BOOL"
        snomed_267036007         = "BOOL"
        snomed_267060006         = "BOOL"
        snomed_267102003         = "BOOL"
        snomed_26929004          = "BOOL"
        snomed_271737000         = "BOOL"
        snomed_271825005         = "BOOL"
        snomed_274531002         = "BOOL"
        snomed_275272006         = "BOOL"
        snomed_278860009         = "BOOL"
        snomed_283371005         = "BOOL"
        snomed_283385000         = "BOOL"
        snomed_283545005         = "BOOL"
        snomed_284549007         = "BOOL"
        snomed_284551006         = "BOOL"
        snomed_288959006         = "BOOL"
        snomed_300916003         = "BOOL"
        snomed_301011002         = "BOOL"
        snomed_302297009         = "BOOL"
        snomed_302870006         = "BOOL"
        snomed_307731004         = "BOOL"
        snomed_30832001          = "BOOL"
        snomed_312157006         = "BOOL"
        snomed_312608009         = "BOOL"
        snomed_314529007         = "BOOL"
        snomed_315268008         = "BOOL"
        snomed_32911000          = "BOOL"
        snomed_33737001          = "BOOL"
        snomed_359817006         = "BOOL"
        snomed_35999006          = "BOOL"
        snomed_361055000         = "BOOL"
        snomed_363406005         = "BOOL"
        snomed_367498001         = "BOOL"
        snomed_368581000119106   = "BOOL"
        snomed_36923009          = "BOOL"
        snomed_36955009          = "BOOL"
        snomed_36971009          = "BOOL"
        snomed_370143000         = "BOOL"
        snomed_370247008         = "BOOL"
        snomed_384709000         = "BOOL"
        snomed_386661006         = "BOOL"
        snomed_38822007          = "BOOL"
        snomed_389087006         = "BOOL"
        snomed_398152000         = "BOOL"
        snomed_398254007         = "BOOL"
        snomed_39848009          = "BOOL"
        snomed_39898005          = "BOOL"
        snomed_399211009         = "BOOL"
        snomed_399261000         = "BOOL"
        snomed_399912005         = "BOOL"
        snomed_40055000          = "BOOL"
        snomed_401303003         = "BOOL"
        snomed_401314000         = "BOOL"
        snomed_40275004          = "BOOL"
        snomed_403190006         = "BOOL"
        snomed_403191005         = "BOOL"
        snomed_403192003         = "BOOL"
        snomed_408512008         = "BOOL"
        snomed_409089005         = "BOOL"
        snomed_414545008         = "BOOL"
        snomed_414667000         = "BOOL"
        snomed_422034002         = "BOOL"
        snomed_422587007         = "BOOL"
        snomed_422650009         = "BOOL"
        snomed_423315002         = "BOOL"
        snomed_424132000         = "BOOL"
        snomed_424393004         = "BOOL"
        snomed_427419006         = "BOOL"
        snomed_428251008         = "BOOL"
        snomed_428915008         = "BOOL"
        snomed_429280009         = "BOOL"
        snomed_431855005         = "BOOL"
        snomed_431856006         = "BOOL"
        snomed_431857002         = "BOOL"
        snomed_433144002         = "BOOL"
        snomed_43724002          = "BOOL"
        snomed_43878008          = "BOOL"
        snomed_44054006          = "BOOL"
        snomed_443165006         = "BOOL"
        snomed_444448004         = "BOOL"
        snomed_444470001         = "BOOL"
        snomed_44465007          = "BOOL"
        snomed_444814009         = "BOOL"
        snomed_446096008         = "BOOL"
        snomed_446654005         = "BOOL"
        snomed_449868002         = "BOOL"
        snomed_4557003           = "BOOL"
        snomed_45816000          = "BOOL"
        snomed_46177005          = "BOOL"
        snomed_47505003          = "BOOL"
        snomed_47693006          = "BOOL"
        snomed_48333001          = "BOOL"
        snomed_48724000          = "BOOL"
        snomed_49436004          = "BOOL"
        snomed_49727002          = "BOOL"
        snomed_49915006          = "BOOL"
        snomed_5251000175109     = "BOOL"
        snomed_53827007          = "BOOL"
        snomed_55680006          = "BOOL"
        snomed_55822004          = "BOOL"
        snomed_56018004          = "BOOL"
        snomed_5602001           = "BOOL"
        snomed_56786000          = "BOOL"
        snomed_57676002          = "BOOL"
        snomed_58150001          = "BOOL"
        snomed_59621000          = "BOOL"
        snomed_60234000          = "BOOL"
        snomed_60573004          = "BOOL"
        snomed_6072007           = "BOOL"
        snomed_62106007          = "BOOL"
        snomed_62564004          = "BOOL"
        snomed_62718007          = "BOOL"
        snomed_64859006          = "BOOL"
        snomed_6525002           = "BOOL"
        snomed_65275009          = "BOOL"
        snomed_65363002          = "BOOL"
        snomed_65710008          = "BOOL"
        snomed_65966004          = "BOOL"
        snomed_66857006          = "BOOL"
        snomed_67782005          = "BOOL"
        snomed_67811000119102    = "BOOL"
        snomed_68235000          = "BOOL"
        snomed_68496003          = "BOOL"
        snomed_68566005          = "BOOL"
        snomed_68962001          = "BOOL"
        snomed_698306007         = "BOOL"
        snomed_698754002         = "BOOL"
        snomed_69896004          = "BOOL"
        snomed_703151001         = "BOOL"
        snomed_706870000         = "BOOL"
        snomed_706893006         = "BOOL"
        snomed_70704007          = "BOOL"
        snomed_713197008         = "BOOL"
        snomed_713458007         = "BOOL"
        snomed_7200002           = "BOOL"
        snomed_72892002          = "BOOL"
        snomed_73430006          = "BOOL"
        snomed_73438004          = "BOOL"
        snomed_73595000          = "BOOL"
        snomed_741062008         = "BOOL"
        snomed_74400008          = "BOOL"
        snomed_75498004          = "BOOL"
        snomed_76571007          = "BOOL"
        snomed_770349000         = "BOOL"
        snomed_78275009          = "BOOL"
        snomed_79586000          = "BOOL"
        snomed_79619009          = "BOOL"
        snomed_80394007          = "BOOL"
        snomed_80583007          = "BOOL"
        snomed_82423001          = "BOOL"
        snomed_83664006          = "BOOL"
        snomed_840539006         = "BOOL"
        snomed_840544004         = "BOOL"
        snomed_84229001          = "BOOL"
        snomed_84757009          = "BOOL"
        snomed_86406008          = "BOOL"
        snomed_86849004          = "BOOL"
        snomed_87433001          = "BOOL"
        snomed_88805009          = "BOOL"
        snomed_90460009          = "BOOL"
        snomed_90560007          = "BOOL"
        snomed_90781000119102    = "BOOL"
        snomed_91302008          = "BOOL"
        snomed_91434003          = "BOOL"
        snomed_91861009          = "BOOL"
        snomed_92691004          = "BOOL"
        snomed_93761005          = "BOOL"
        snomed_94260004          = "BOOL"
        snomed_95417003          = "BOOL"
        snomed_97331000119101    = "BOOL"
      }
    }

    // Flags whether patient has active medication identified by its RxNorm code.
    // Featurestore indexes features by an ID and a timestamp to reflect
    // what is known about a feature at a given time. A tuple of a patient's
    // id, timestamp and medication flags represents known active medications
    // of a patient at that point in time.
    // See https://www.nlm.nih.gov/research/umls/rxnorm/index.html for more
    // details about RxNorm codes.
    medications = {
      description = "Flags whether patient has active medication identified by its RxNorm code"
      features    = {
        rxnorm_1000126 = "BOOL"
        rxnorm_108515  = "BOOL"
        rxnorm_1116635 = "BOOL"
        rxnorm_1234995 = "BOOL"
        rxnorm_1361048 = "BOOL"
        rxnorm_1361226 = "BOOL"
        rxnorm_1601380 = "BOOL"
        rxnorm_1605257 = "BOOL"
        rxnorm_1659131 = "BOOL"
        rxnorm_1659149 = "BOOL"
        rxnorm_1659263 = "BOOL"
        rxnorm_1664986 = "BOOL"
        rxnorm_1665060 = "BOOL"
        rxnorm_1719286 = "BOOL"
        rxnorm_1723208 = "BOOL"
        rxnorm_1728805 = "BOOL"
        rxnorm_1729584 = "BOOL"
        rxnorm_1732136 = "BOOL"
        rxnorm_1732186 = "BOOL"
        rxnorm_1734919 = "BOOL"
        rxnorm_1735006 = "BOOL"
        rxnorm_1736776 = "BOOL"
        rxnorm_1736854 = "BOOL"
        rxnorm_1737466 = "BOOL"
        rxnorm_1740467 = "BOOL"
        rxnorm_1790099 = "BOOL"
        rxnorm_1796676 = "BOOL"
        rxnorm_1803932 = "BOOL"
        rxnorm_1804799 = "BOOL"
        rxnorm_1807510 = "BOOL"
        rxnorm_1808217 = "BOOL"
        rxnorm_1809104 = "BOOL"
        rxnorm_1856546 = "BOOL"
        rxnorm_1860480 = "BOOL"
        rxnorm_1873983 = "BOOL"
        rxnorm_1946840 = "BOOL"
        rxnorm_198039  = "BOOL"
        rxnorm_198240  = "BOOL"
        rxnorm_198440  = "BOOL"
        rxnorm_199224  = "BOOL"
        rxnorm_1997015 = "BOOL"
        rxnorm_200243  = "BOOL"
        rxnorm_200349  = "BOOL"
        rxnorm_205923  = "BOOL"
        rxnorm_2119714 = "BOOL"
        rxnorm_212033  = "BOOL"
        rxnorm_2123111 = "BOOL"
        rxnorm_226719  = "BOOL"
        rxnorm_238100  = "BOOL"
        rxnorm_242969  = "BOOL"
        rxnorm_309845  = "BOOL"
        rxnorm_310261  = "BOOL"
        rxnorm_311034  = "BOOL"
        rxnorm_311700  = "BOOL"
        rxnorm_312617  = "BOOL"
        rxnorm_313212  = "BOOL"
        rxnorm_389221  = "BOOL"
        rxnorm_542347  = "BOOL"
        rxnorm_562366  = "BOOL"
        rxnorm_583214  = "BOOL"
        rxnorm_597195  = "BOOL"
        rxnorm_727711  = "BOOL"
        rxnorm_727762  = "BOOL"
        rxnorm_749196  = "BOOL"
        rxnorm_752899  = "BOOL"
        rxnorm_807283  = "BOOL"
        rxnorm_854235  = "BOOL"
        rxnorm_854252  = "BOOL"
        rxnorm_854255  = "BOOL"
        rxnorm_855812  = "BOOL"
        rxnorm_979485  = "BOOL"
        rxnorm_999999  = "BOOL"
      }
    }

    // Flags as 'LOW', 'MID', 'HIGH' patient's observations and measurements
    // identified by its Loinc Code.
    // Featurestore indexes features by an ID and a timestamp to reflect
    // what is known about a feature at a given time. A tuple of a patient's
    // id, timestamp and observations represents the latest known value
    // of a patient at that point in time.
    // See https://loinc.org/ for more information about Loinc codes.
    observations = {
      description = "Flags whether patient has active medication identified by its Loinc code"
      features    = {
        loinc_10230_1 = "STRING"
        loinc_10480_2 = "STRING"
        loinc_10834_0 = "STRING"
        loinc_14627_4 = "STRING"
        loinc_14804_9 = "STRING"
        loinc_14959_1 = "STRING"
        loinc_1742_6  = "STRING"
        loinc_1751_7  = "STRING"
        loinc_17861_6 = "STRING"
        loinc_18262_6 = "STRING"
        loinc_18752_6 = "STRING"
        loinc_19123_9 = "STRING"
        loinc_1920_8  = "STRING"
        loinc_1960_4  = "STRING"
        loinc_1975_2  = "STRING"
        loinc_1988_5  = "STRING"
        loinc_19926_5 = "STRING"
        loinc_19994_3 = "STRING"
        loinc_2019_8  = "STRING"
        loinc_2021_4  = "STRING"
        loinc_2027_1  = "STRING"
        loinc_2028_9  = "STRING"
        loinc_20447_9 = "STRING"
        loinc_20454_5 = "STRING"
        loinc_20505_4 = "STRING"
        loinc_20565_8 = "STRING"
        loinc_20570_8 = "STRING"
        loinc_2069_3  = "STRING"
        loinc_2075_0  = "STRING"
        loinc_2085_9  = "STRING"
        loinc_2093_3  = "STRING"
        loinc_21000_5 = "STRING"
        loinc_21377_7 = "STRING"
        loinc_2157_6  = "STRING"
        loinc_2160_0  = "STRING"
        loinc_21905_5 = "STRING"
        loinc_21906_3 = "STRING"
        loinc_21907_1 = "STRING"
        loinc_21908_9 = "STRING"
        loinc_21924_6 = "STRING"
        loinc_2276_4  = "STRING"
        loinc_2339_0  = "STRING"
        loinc_2345_7  = "STRING"
        loinc_24467_3 = "STRING"
        loinc_2498_4  = "STRING"
        loinc_2500_7  = "STRING"
        loinc_2502_3  = "STRING"
        loinc_2514_8  = "STRING"
        loinc_2532_0  = "STRING"
        loinc_25428_4 = "STRING"
        loinc_2571_8  = "STRING"
        loinc_26453_1 = "STRING"
        loinc_26464_8 = "STRING"
        loinc_26515_7 = "STRING"
        loinc_2703_7  = "STRING"
        loinc_2705_2  = "STRING"
        loinc_2708_6  = "STRING"
        loinc_2713_6  = "STRING"
        loinc_2744_1  = "STRING"
        loinc_2746_6  = "STRING"
        loinc_2777_1  = "STRING"
        loinc_2823_3  = "STRING"
        loinc_28245_9 = "STRING"
        loinc_2857_1  = "STRING"
        loinc_2885_2  = "STRING"
        loinc_29463_7 = "STRING"
        loinc_2947_0  = "STRING"
        loinc_2951_2  = "STRING"
        loinc_29554_3 = "STRING"
        loinc_3016_3  = "STRING"
        loinc_3024_7  = "STRING"
        loinc_30385_9 = "STRING"
        loinc_30428_7 = "STRING"
        loinc_3094_0  = "STRING"
        loinc_3173_2  = "STRING"
        loinc_3184_9  = "STRING"
        loinc_32167_9 = "STRING"
        loinc_32207_3 = "STRING"
        loinc_32465_7 = "STRING"
        loinc_32623_1 = "STRING"
        loinc_32693_4 = "STRING"
        loinc_33037_3 = "STRING"
        loinc_33728_7 = "STRING"
        loinc_33756_8 = "STRING"
        loinc_33762_6 = "STRING"
        loinc_33914_3 = "STRING"
        loinc_33959_8 = "STRING"
        loinc_34533_0 = "STRING"
        loinc_38208_5 = "STRING"
        loinc_38265_5 = "STRING"
        loinc_38483_4 = "STRING"
        loinc_39156_5 = "STRING"
        loinc_42719_5 = "STRING"
        loinc_44261_6 = "STRING"
        loinc_44667_4 = "STRING"
        loinc_44963_7 = "STRING"
        loinc_4544_3  = "STRING"
        loinc_4548_4  = "STRING"
        loinc_46240_8 = "STRING"
        loinc_46288_7 = "STRING"
        loinc_48065_7 = "STRING"
        loinc_49765_1 = "STRING"
        loinc_55277_8 = "STRING"
        loinc_55758_7 = "STRING"
        loinc_5767_9  = "STRING"
        loinc_5770_3  = "STRING"
        loinc_5778_6  = "STRING"
        loinc_57905_2 = "STRING"
        loinc_5792_7  = "STRING"
        loinc_5794_3  = "STRING"
        loinc_5797_6  = "STRING"
        loinc_5799_2  = "STRING"
        loinc_5802_4  = "STRING"
        loinc_5803_2  = "STRING"
        loinc_5804_0  = "STRING"
        loinc_5811_5  = "STRING"
        loinc_5902_2  = "STRING"
        loinc_5905_5  = "STRING"
        loinc_59408_5 = "STRING"
        loinc_59460_6 = "STRING"
        loinc_59461_4 = "STRING"
        loinc_59557_9 = "STRING"
        loinc_59576_9 = "STRING"
        loinc_6075_6  = "STRING"
        loinc_6082_2  = "STRING"
        loinc_6085_5  = "STRING"
        loinc_6095_4  = "STRING"
        loinc_6106_9  = "STRING"
        loinc_6158_0  = "STRING"
        loinc_6189_5  = "STRING"
        loinc_6206_7  = "STRING"
        loinc_6246_3  = "STRING"
        loinc_6248_9  = "STRING"
        loinc_6273_7  = "STRING"
        loinc_6276_0  = "STRING"
        loinc_6298_4  = "STRING"
        loinc_6299_2  = "STRING"
        loinc_6301_6  = "STRING"
        loinc_63513_6 = "STRING"
        loinc_65750_2 = "STRING"
        loinc_66519_0 = "STRING"
        loinc_66524_0 = "STRING"
        loinc_66529_9 = "STRING"
        loinc_66534_9 = "STRING"
        loinc_6690_2  = "STRING"
        loinc_6768_6  = "STRING"
        loinc_6833_8  = "STRING"
        loinc_6844_5  = "STRING"
        loinc_70006_2 = "STRING"
        loinc_70274_6 = "STRING"
        loinc_704_7   = "STRING"
        loinc_706_2   = "STRING"
        loinc_711_2   = "STRING"
        loinc_713_8   = "STRING"
        loinc_71425_3 = "STRING"
        loinc_718_7   = "STRING"
        loinc_71802_3 = "STRING"
        loinc_72091_2 = "STRING"
        loinc_72106_8 = "STRING"
        loinc_72166_2 = "STRING"
        loinc_72514_3 = "STRING"
        loinc_7258_7  = "STRING"
        loinc_731_0   = "STRING"
        loinc_736_9   = "STRING"
        loinc_74006_8 = "STRING"
        loinc_742_7   = "STRING"
        loinc_751_8   = "STRING"
        loinc_75325_1 = "STRING"
        loinc_75443_2 = "STRING"
        loinc_75626_2 = "STRING"
        loinc_76504_0 = "STRING"
        loinc_76690_7 = "STRING"
        loinc_770_8   = "STRING"
        loinc_77606_2 = "STRING"
        loinc_777_3   = "STRING"
        loinc_785_6   = "STRING"
        loinc_786_4   = "STRING"
        loinc_787_2   = "STRING"
        loinc_788_0   = "STRING"
        loinc_789_8   = "STRING"
        loinc_80271_0 = "STRING"
        loinc_80382_5 = "STRING"
        loinc_80383_3 = "STRING"
        loinc_82667_7 = "STRING"
        loinc_8289_1  = "STRING"
        loinc_8302_2  = "STRING"
        loinc_8310_5  = "STRING"
        loinc_8331_1  = "STRING"
        loinc_84215_3 = "STRING"
        loinc_8478_0  = "STRING"
        loinc_85318_4 = "STRING"
        loinc_85319_2 = "STRING"
        loinc_85337_4 = "STRING"
        loinc_85339_0 = "STRING"
        loinc_85343_2 = "STRING"
        loinc_85344_0 = "STRING"
        loinc_85352_3 = "STRING"
        loinc_85354_9 = "STRING"
        loinc_86923_0 = "STRING"
        loinc_88020_3 = "STRING"
        loinc_88021_1 = "STRING"
        loinc_88040_1 = "STRING"
        loinc_88262_1 = "STRING"
        loinc_8867_4  = "STRING"
        loinc_89204_2 = "STRING"
        loinc_89579_7 = "STRING"
        loinc_91148_7 = "STRING"
        loinc_92130_4 = "STRING"
        loinc_92131_2 = "STRING"
        loinc_92134_6 = "STRING"
        loinc_92138_7 = "STRING"
        loinc_92139_5 = "STRING"
        loinc_92140_3 = "STRING"
        loinc_92141_1 = "STRING"
        loinc_92142_9 = "STRING"
        loinc_9279_1  = "STRING"
        loinc_93025_5 = "STRING"
        loinc_94040_3 = "STRING"
        loinc_94531_1 = "STRING"
        loinc_9843_4  = "STRING"
        loinc_99999_0 = "STRING"
        loinc_x9999_0 = "STRING"
      }
    }
  }
}

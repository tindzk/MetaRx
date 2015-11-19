# Development
[![Build Status](https://travis-ci.org/MetaStack-pl/MetaRx.svg)](https://travis-ci.org/MetaStack-pl/MetaRx)

## Tests
The proper functioning of each operation is backed by [test cases](https://github.com/MetaStack-pl/MetaRx/tree/master/shared/src/test/scala/pl/metastack/metarx). These also provide complementary documentation.

## Manual
Run the following command to generate the manual:

```bash
$ sbt manual/runMain pl.metastack.metarx.manual.Manual
```

Deploy it with:

```bash
$ sbt manual/runMain pl.metastack.metarx.manual.Deploy
```

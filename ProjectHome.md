## Intro ##

BigSTRUCTURE is a scalable backend infrastructure where Processing Power is detached but guaranteed, we enable applications that can seamlessly run in 1 or 1000 machines.

BigSTRUCTURE depends heavily on Apache ZooKeeper (although a test non-zookeeper implementation is planned) and allows you to use prototype-like code for production.

## Summary ##
There are plenty of available solutions for the distributed app problem and all of them fail to be easy to deploy and are reduced to very specific sub-domains.

For example, MapReduce is the standard for processing big data, but it doesn't work with data streams, needs pre-formatted static data. On the opposite spectrum we have Complex Event Processing, these solutions allow scalable systems able to process streams of data, but the code is static and typical CEP processors are used to raise events which are then acted upon by yet another layer.

This features are meant to be added values once we have `big data`, they are also mostly useless if we have `small data`.

This is an issue in modern days where viral apps can need to be ready to
deal with floods of users.

BigSTRUCTURE's is to help those indies and startups that need an easy to deploy yet scalable solution.

With BigSTRUCTURE you can make simple yet 'viral proof' prototypes, it's both easy to develop and scale.

[Video](https://www.youtube.com/watch?v=KPxyAmUfX7g)


## How ##

Moving from prototype to large scale is usually complex not because of the task but the amount of tools and development challenges that meet the developers. However to develop a large scale BigSTRUCTURE app (or a simple one) all you need to know is the traditional server/client/socket paradigm.

BigSTRUCTURE uses a trick we called EPU (Ephemeral Processing Unit) and replaces the traditional hostname (for hardware nodes) by virtually unlimited hostnodes ( unique strings that can be attached to any number of EPUs).

With BigSTRUCTURE you always program for big data, even if just on your laptop.

See also:
  * [Example 1](https://code.google.com/p/bigstructure/wiki/Example1)
  * [Tutorial 1](https://code.google.com/p/bigstructure/wiki/Tutorial1)

## Roadmap ##
**v0.6**
  * multepu (multiple EPUs per service)
  * dynamically submit Services (a core _admin_ service that permits adding/listing/deleting services)

**v0.7**
  * Pure Java replacement of Apache ZooKeeper for development or very small environments

**v0.8+**
  * Create more demonstration examples

### History ###
**v0.4**
  * Jar file capable of being deployed
  * Groovy based client (./bigstructure.jar client <groovy client file>

**v0.5**
  * automatic discard of EPUs for timedout services
  * implement `waitForEPU(path,timeout)` - wait for an EPU to become available or for `timeout` milliseconds
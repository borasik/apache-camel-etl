https://github.com/prometheus-operator/prometheus-operator/blob/master/Documentation/additional-scrape-config.md
https://prometheus.io/docs/prometheus/latest/configuration/configuration/#scrape_config

Follow the instructions in the link above to setup a camel-metrics for al pods that expose a named containerPort at TCP:9779 named 'camel-metrics'

``` bash
$ kubectl create secret generic additional-scrape-configs --from-file=prometheus-additional.yaml
```
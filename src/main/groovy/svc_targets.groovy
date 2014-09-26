package com.go2uti.bandd

import org.gradle.api.*

class svc_targets {
    def parse_config(service_map) {
        def config_map = [:]

        /* 
           config_map = 
           [service1: [type: stype, config: [host1: [port1, port2, port3, portN],
                                             host2: [port1, portN]]],
           service2: [type: stype, config: [host1: [node1, node2],
                                            host2: [node1, nodeN]]]]
        */
        // iterate across services
        service_map.each { service, config ->
            // throw away service type
            def service_name = service - '_DEPLOY_TARGETS'
            def service_type = config.tokenize(':')[0] - ';'
            def hostlist = config.tokenize(':')[1]?.trim()
            def service_config

            switch (service_type) {
                case "AMX":
                    service_config = parse_amx_config(service, hostlist)
                    break
                case "BE":
                    service_config = parse_be_config(service, hostlist)
                    break
                case "BE_FILES":
                    service_config = parse_befiles_config(service, hostlist)
                    break
                case ["IGNORE","NOT_YET_DEPLOYED","JRUBY","JRUBY_HELP","BIPUB","CLASSIC","POJO","FICO","QUARTZ"]:
                    service_config = parse_ignore_config(service, hostlist)
                    break

                default:
                    throw new GradleException("No config parser for service type ${service_type}")
            }
            config_map[service_name] = [type: service_type, config: service_config]
        }
        return config_map
    }

    def parse_amx_config(service, hostlist) {
        //EIT_1World_CO_CustomsOperationTaskService_UnWired_SOA_DEPLOY_TARGETS=AMX:sinupy54.go2uti.com,A02A;sinupy63.go2uti.com,B02B;sinupy57.go2uti.com,A02B;sinupy60.go2uti.com,B02A;
        //   [service1: [host1: [node1, node2, node3, nodeN],
        //               host2: [node1, node2] ]
        // iterate across hosts
        def host_node_map = [:]
        hostlist.tokenize(';').each { i ->
            def (host, nodes) = i.tokenize(',')
                if (nodes == null) throw new GradleException("AMX Service ${service}: no nodes defined in svc_targets file for host ${host}")
                    def node_list = nodes.tokenize('|')
                        host_node_map[host] =  node_list
        }
        return host_node_map
    }

    def parse_be_config(service, hostlist) {
        // CO_BE_Choreography_Service_DEPLOY_TARGETS=BE:sinupy41.go2uti.com,8105;sinupy42.go2uti.com,8105;sinupy43.go2uti.com,8105;sinupy44.go2uti.com,8105;
        //   [service1: [host1: [port1, port2, port3, portN],
        //               host2: [port1, portN] ]

        // iterate across hosts
        def host_port_map = [:]
        hostlist.tokenize(';').each { i ->
            def (host, ports) = i.tokenize(',')
                if (ports == null) throw new GradleException("Service ${service}: no ports defined in svc_targets file for host ${host}")
                    def port_list = ports.tokenize('|')
                        host_port_map[host] =  port_list
        }
        return host_port_map
    }

    def parse_befiles_config(service, hostlist) {
        // BE_Shared_DEPLOY_TARGETS=BE_FILES:sinupy41.go2uti.com;sinupy42.go2uti.com;sinupy43.go2uti.com;sinupy44.go2uti.com;sinupy45.go2uti.com;sinupy46.go2uti.com;sinupy47.go2uti.com;sinupy48.go2uti.com;
        //   [service1: [host1, host2, hostN]]

        // iterate across hosts
        def host_list = []
        hostlist.tokenize(';').each { host ->
            host_list << host
        }
        return host_list
    }

    def parse_ignore_config(service, hostlist) {
        return [:]
    }

    def load_svc_targets(basedir, env) {
        // define mapping between ENV_PROP_PREFIX property and svc_targets naming (probably should just pick a scheme one day)
        def env_map = ['PROD_SIN':   'prodsin',
                       'DEVINT':     'devint',
                       'DEVINT2':    'devint2',
                       'SYSINT':     'sysint',
                       'QA_FRA':     'qcfra',
                       'QA':         'qcsin',
                       'UAT':        'uat',
                       'STAGING_FRA':'stagingfra',
                       'DEVOPS':     'devops']

        // attempt to load the property file based on the given enviroment 
        def props = new Properties()
        new File("${basedir}/${env_map[env]}_svc_targets.properties").withInputStream {
            stream -> props.load(stream)
        }

        // load service targets property file in map
        return props
    }

    /* 
    task test() << {
        // load service targets property file in map
        def svc_target_map = load_svc_targets("../..", 'PROD_SIN')

        def be_svc_script_map = ['BE_Cache_Service_DEPLOY_TARGETS':           'becomcache',
                                 'CO_BE_Choreography_Service_DEPLOY_TARGETS': 'becocho',
                                 'CO_BE_CRUD_Coarse_Service_DEPLOY_TARGETS':  'becocoarse',
                                 'CO_BE_Validation_Service_DEPLOY_TARGETS':   'becoval',
                                 'CO_BE_Sync_Service_DEPLOY_TARGETS':         'becosynch',
                                 'GI_BE_Choreography_Service_DEPLOY_TARGETS': 'begicho',
                                 'GI_BE_CRUD_Service_DEPLOY_TARGETS':         'begicud'] 

        def amx_svc_script_set =   ['EIT_1World_CO_FreightUITaskService_SOA_DEPLOY_TARGETS',
                                    'EIT_1World_CO_FacadeMediation_SOA_DEPLOY_TARGETS',
                                    'EIT_1World_CO_FreightContainerTaskService_SOA_DEPLOY_TARGETS',
                                    'EIT_1World_CO_FreightEntityService_SOA_DEPLOY_TARGETS',
                                    'EIT_1World_CO_FreightMovementTaskService_SOA_DEPLOY_TARGETS',
                                    'EIT_1World_CO_FreightOperationsTaskService_SOA_DEPLOY_TARGETS',
                                    'EIT_1World_CO_FreightTaskService_SOA_DEPLOY_TARGETS',
                                    'EIT_1World_CO_FreightUITaskService_SOA_DEPLOY_TARGETS'] as Set

        // filter the service targets property file to just the BE services
        def be_svc_target_map = svc_target_map.subMap(be_svc_script_map.keySet())
        // parse the key/value pairs that are left k=v, and make a hierarchical map with everything we need
        def be_config_map = parse_config(be_svc_target_map)
        println "BE Config: " + be_config_map
        println ""

        // filter the service targets property file to just the AMX services
        def amx_svc_target_map = svc_target_map.subMap(amx_svc_script_set)
        // parse the key/value pairs that are left k=v, and make a hierarchical map with everything we need
        def amx_config_map = parse_config(amx_svc_target_map)
        println "AMX Config:" + amx_config_map
        println ""

        def both_svc_target_map = be_svc_target_map + amx_svc_target_map
        def both_config_map = parse_config(both_svc_target_map)
        println "Both together:" + both_config_map

        def all_config_map = parse_config(svc_target_map)
        println "Everything:" + all_config_map
    }
    */
}

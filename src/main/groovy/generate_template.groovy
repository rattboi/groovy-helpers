package com.go2uti.bandd

import org.gradle.api.*
import groovy.text.GStringTemplateEngine

class template_generator {
    def generate_template(serviceName, serviceConfig) {
        println serviceName
        println serviceConfig

        def serviceType = serviceConfig[serviceName]['type']

        def gen_template

        switch (serviceType) {
            case "AMX":
                gen_template = generate_amx_template(serviceConfig, 'A')
                break
            default:
                throw new GradleException("No template generator for service type ${serviceType}")
        }

        def engine = new GStringTemplateEngine()
        def binding = ["env":"PROD-SIN", "date":"10/10/10", "release":"private", "service": serviceName]

        //def heading = new File('generate_template/heading.template')
        String heading = this.getClass().getResource( '/heading.template' ).text
        def header_template = engine.createTemplate(heading).make(binding)
        println header_template.toString()

        //def global_options = new File('generate_template/global_options.snippet')
        String global_options = this.getClass().getResource( '/global_options.snippet' ).text
        global_options.eachLine {  line -> println(line) }

        // now put our service lines out there
        gen_template.each { line -> println(line) }
    }

    def get_amx_node_options(nodeCount, nodeTotal) {
        def nodeOptions = ["\${AMX_MULTINODE_DEPLOY_FIRST_NODE}",
                           "\${AMX_MULTINODE_DEPLOY_MIDDLE_NODE}",
                           "\${AMX_MULTINODE_DEPLOY_LAST_NODE}"]

        if (nodeCount == 1)  // First node
            return nodeOptions[0]
        else if (nodeCount == nodeTotal) // Last node
            return nodeOptions[2]
        else  // Middle node
            return nodeOptions[1]
    }

    def get_node_count(hosts) { 
        return hosts.values().flatten().size()
    }

    def filter_hosts_for_stack(hosts, stackSide) {
        hosts.inject([:]) { new_map, host, nodes -> 
            if (nodes.every { it.startsWith(stackSide) } ) { new_map[host] = nodes} 
            return new_map
        }
    }

    def generate_amx_service_lines(serviceName, hosts, nodeTotal) {
        def nodeCount = 1
        def serviceLines = []
        hosts.each { host, nodes ->
            nodes.each { node ->
                serviceLines << "${serviceName}_LIST_${nodeCount}=PROCESS_ONE_PROJECT=${serviceName};${get_amx_node_options(nodeCount++, nodeTotal)};\${APP_AND_EMS_DEPLOY};AMX_NODE_NAME=${node};AMX_HOST_NAME=${host}"
            }
        }
        return serviceLines
    }

    def generate_amx_template(serviceConfig, stackSide) {

        def serviceName = serviceConfig.keySet()[0]
        def hosts = serviceConfig[serviceName]['config']

        // filter hosts to just the hosts related to the stack side we're dealing with
        def stackSideHosts = filter_hosts_for_stack(hosts, stackSide)

        // Count total nodes for a specific stack side
        def nodeTotal = get_node_count(stackSideHosts)

        // Generate the templater lines
        generate_amx_service_lines(serviceName, stackSideHosts, nodeTotal)
    }
}

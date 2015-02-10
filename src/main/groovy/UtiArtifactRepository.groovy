package com.go2uti.bandd

import org.apache.commons.net.ftp.FTPClient
import org.gradle.api.*

/*
	UtiArtifactRepository Class
	- Used for interacting with the Artifact Repository
*/
class UtiArtifactRepository {
    FTPClient ftpClient;

    String hostname
    String username
    String password
    int port

    UtiArtifactRepository(String hostname, String username, String password, int port) {
        ftpClient = new FTPClient()
        this.hostname = hostname
        this.username = username
        this.password = password
        this.port     = port
    }

    UtiArtifactRepository(String hostname, String username, String password) {
        this(hostname, username, password, 21)
    }

    void connect() {
        try {
            ftpClient.connect hostname, port
        } catch (Exception e) {

        }
        if (ftpClient.getReplyCode() != 220) throw new GradleException("Couldn't connect to Artifact Repository FTP: ${hostname}")
        ftpClient.enterLocalPassiveMode()
        ftpClient.login username, password
        if (ftpClient.getReplyCode() != 230) throw new GradleException("Unable to authenticate to Artifact Repository FTP with user ${username}")
    }

    void disconnect() {
        ftpClient.disconnect()
    }

    boolean checkForBuildInRepo(stream, component, project, buildLabel) {
        ftpClient.changeWorkingDirectory("/DevSupportArtifacts-Prod/${stream}/${component}/${project}/${buildLabel}")
        return (ftpClient.getReplyCode() == 250)
    }
}

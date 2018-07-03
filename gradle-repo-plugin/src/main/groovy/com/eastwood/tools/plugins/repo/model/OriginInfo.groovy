package com.eastwood.tools.plugins.repo.model

class OriginInfo {

    String url
    String path
    boolean isSSH

    String ssh_url_to_repo
    String http_url_to_repo

    String getOriginUrl() {
        return url + (isSSH ? ":" : "") + path
    }

}
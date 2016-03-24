# -*- mode: ruby -*-
# vi: set ft=ruby :

VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
    config.vm.define "scalawiki", primary: true do |scalawiki|
        scalawiki.vm.box = "ubuntu/trusty64"
        scalawiki.vm.provision "shell", path: "provision.sh"
    end

    config.vm.provider "virtualbox" do |vb|
        vb.memory = 2048
    end
end
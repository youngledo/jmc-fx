package com.youngledo.jmcfx.packager;

import java.util.List;

interface CommandRunner {
    void run(List<String> command) throws Exception;
}

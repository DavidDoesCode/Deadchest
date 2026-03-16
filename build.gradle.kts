allprojects {
    group = "me.crylonz.deadchest"
    version = "4.27.0"
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}

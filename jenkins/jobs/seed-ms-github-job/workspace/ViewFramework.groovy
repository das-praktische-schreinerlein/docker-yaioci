class ViewFramework {
    def dslFactory;

    def ViewFramework(def dslFactory) {
        this.dslFactory = dslFactory;
    }

    def createDefaultView(def viewName, def jobRNameRegEx) {
        dslFactory.listView(viewName) {
            description('')
            filterBuildQueue()
            filterExecutors()
            jobs {
                regex(jobRNameRegEx)
            }
            columns {
                status()
                buildButton()
                weather()
                name()
                lastSuccess()
                lastFailure()
                lastDuration()
            }
        }
    }
}

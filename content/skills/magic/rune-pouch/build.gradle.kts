plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    integrationImplementation(projects.content.interfaces.bank)
    integrationImplementation(projects.api.combat.combatWeapon)
}

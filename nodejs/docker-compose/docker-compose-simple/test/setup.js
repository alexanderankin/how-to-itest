import path from 'node:path'

import { setup, teardown } from 'mocha';
import {
    PostgreSqlContainer,
    StartedPostgreSqlContainer
} from "@testcontainers/postgresql";
import {
    DockerComposeEnvironment,
    StartedDockerComposeEnvironment
} from "testcontainers"

let container;

/**
 *
 * @returns {StartedDockerComposeEnvironment}
 */
export function getContainer() {
    return container;
}

// let __dirname = path.dirname(url.fileURLToPath(import.meta.url))

setup(async function () {
    this.timeout(30_000)
    console.log('hello from setup')
    container = await new DockerComposeEnvironment(__dirname, 'compose.yaml').up();
})

teardown(() => {
    console.log('hello from teardown')
})

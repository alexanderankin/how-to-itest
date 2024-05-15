import { knex } from "./db.js";
import { ExampleService } from "./example-service.js";

const exampleService = new ExampleService(knex);

export { exampleService }

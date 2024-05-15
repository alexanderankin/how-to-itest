import { getContainer } from "./setup.js";

describe('test-example.js', () => {
    it('should show an example', () => {
        console.log(getContainer().getContainer('db').getFirstMappedPort())
    })
})

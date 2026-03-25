import { gql } from '@apollo/client';

export const LOGIN_USER = gql`
mutation LoginUser($email: String!, $password: String!) {
    loginUser(email: $email, password: $password) {
        errors
        user{
            confirmedAt
            createdAt
            email
            id
            image
            name
            nickname
            provider
            uid
            updatedAt
        }
    }
}
`


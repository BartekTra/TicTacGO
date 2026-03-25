import { gql } from '@apollo/client';

export const REGISTER_USER = gql`
  mutation RegisterUser($nickname: String!, $name: String!, $email: String!, $password: String!, $passwordConfirmation: String!) {
    registerUser(
      nickname: $nickname,
      name: $name,
      email: $email, 
      password: $password, 
      passwordConfirmation: $passwordConfirmation 
    ) {
      user {
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
      errors
    }
  }`


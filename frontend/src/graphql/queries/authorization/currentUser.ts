
import { gql } from '@apollo/client';

export const CURRENT_USER = gql`
query CurrentUser {
  currentUser {
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
}`

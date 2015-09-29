/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.copycat.client.response;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.SerializeWith;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.util.Assert;
import io.atomix.catalyst.util.BuilderPool;
import io.atomix.catalyst.util.ReferenceManager;
import io.atomix.copycat.client.error.RaftError;

import java.util.Objects;

/**
 * Protocol connect client response.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
@SerializeWith(id=279)
public class ConnectResponse extends SessionResponse<ConnectResponse> {

  private static final BuilderPool<Builder, ConnectResponse> POOL = new BuilderPool<>(Builder::new);

  /**
   * Returns a new connect client response builder.
   *
   * @return A new connect client response builder.
   */
  public static Builder builder() {
    return POOL.acquire();
  }

  /**
   * Returns a connect client response builder for an existing response.
   *
   * @param response The response to build.
   * @return The connect client response builder.
   * @throws NullPointerException if {@code response} is null
   */
  public static Builder builder(ConnectResponse response) {
    return POOL.acquire(Assert.notNull(response, "response"));
  }

  /**
   * @throws NullPointerException if {@code referenceManager} is null
   */
  public ConnectResponse(ReferenceManager<ConnectResponse> referenceManager) {
    super(referenceManager);
  }

  @Override
  public void readObject(BufferInput buffer, Serializer serializer) {
    status = Status.forId(buffer.readByte());
    if (status == Status.OK) {
      error = null;
    } else {
      error = RaftError.forId(buffer.readByte());
    }
  }

  @Override
  public void writeObject(BufferOutput buffer, Serializer serializer) {
    buffer.writeByte(status.id());
    if (status == Status.ERROR) {
      buffer.writeByte(error.id());
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass(), status);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof ConnectResponse) {
      ConnectResponse response = (ConnectResponse) object;
      return response.status == status;
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("%s[status=%s]", getClass().getSimpleName(), status);
  }

  /**
   * Register response builder.
   */
  public static class Builder extends SessionResponse.Builder<Builder, ConnectResponse> {

    /**
     * @throws NullPointerException if {@code pool} is null
     */
    protected Builder(BuilderPool<Builder, ConnectResponse> pool) {
      super(pool, ConnectResponse::new);
    }
  }

}
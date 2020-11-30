package org.folio.pv.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class PwnedClientAddPaddingInterceptor implements RequestInterceptor {

  private static final String ADD_PADDING_HEADER = "Add-Padding";

  private final boolean paddingEnabled;


  public PwnedClientAddPaddingInterceptor(boolean paddingEnabled) {
    this.paddingEnabled = paddingEnabled;
  }

  @Override
  public void apply(RequestTemplate template) {
    if (paddingEnabled) {
      template.header(ADD_PADDING_HEADER, "true");
    }
  }
}